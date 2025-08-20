# E-commerce Asynchronous Design

> `Redis` 기반으로 비즈니스 로직 '선착순'이 보장되는 기능을 설계한다. 해당 설계를 기반으로 개발 및 구현한다.

## Redis 기반 선착순 쿠폰 발급 설계

### 문제 정의

- 기존에 조건부 업데이트 쿼리를 통해서 초과 발급은 방지하였으나, 선착순은 보장되지 않음
- 트래픽이 몰리는 상황에서 Redis를 활용한 분산환경 및 선착순 제어 로직 추가 필요

### 설계 목표

1. 선착순 보장: 초과된 요청에 대하여 즉시 차단
2. 비동기 확정: 쿠폰 엔티티를 생성하는 확정 로직은 큐/비동기 프로세스에서 수행
3. 장애 격리: Redis 장애 시 우아한 폴백(graceful fallback) 설계 - 학습

### Key & 자료구조

- 남은 수량 관리
  - key: `COUPON:POLICY:{policyId}:REMAINING`
  - 자료구조: `String` 에 최초 발급 수량 저장
    - `DECR`로 원자적 차감, 0 미만 시 마감 처리
  - `TTL`: `CouponPolicy.endedAt` 기준으로 `expire` 설정 → 이벤트 종료 시 자동 정리

- 선착순 관리
  - key: `COUPON:POLICY:{policyId}:PENDING`
  - 자료구조: `ZSET`(`userId`, `timestamp`)
    - Redis 선착순 통과 직후 사용자를 넣음
    - score: 요청 시간(ms + 랜덤숫자) 저장 → "누가 몇 번째로 도착했는지" 순서 관리 가능
      - `ms`까지 동일한 경우를 대비해서, `ms + 랜덤 숫자`를 지정해야할까? 어느정도 중복이 되나 
      - ms까지 동일한데, 1장 남았을 경우가 과연 많을까 고민하였지만, 설계는 중복되지 않는 것을 목표로 작성했습니다.
    - consumer가 DB 반영 후 ISSUED로 이동.
  - `TTL`
    - `CouponPolicy.endedAt` 이후 자동 삭제(기본)
    - `Worker`가 주기적(10초 미만)으로 `poll` → DB 반영 완료 후 `ISSUED`로 이동
    - (질문): PENDING에 있는 값도 쿠폰 정책 종료 일자로 만료를 지정하는 것과 워커 실행 주기보다만 길게 가져가는 것에 대해 고민했습니다.
      - 워커에서 100~1000개씩 처리를 한다고 하더라도, 

- 발급 확정
  - key: `COUPON:POLICY:{policyId}:ISSUED`
  - 자료구조: `SET`(`userId`)
    - DB 반영 완료된 사용자
    - 중복 요청 차단 및 발급 최종 이력 확인용
  - `TTL`: `CouponPolicy.endedAt` 기준으로 `expire` 설정

### 서비스 계층 개선

- 현재 구조에서의 쿠폰 발급 흐름 
  > CouponController → CouponFacade → CouponService → CouponPolicyRepository 
  - 쿠폰 발급 요청이 그대로 DB에 반영됨.
  - `CouponPolicyRepository`에서 남은 수량을 관리하며, 조건부 업데이트 쿼리로 초과 발급을 방지함
  - DB에 직접 부하가 발생하며, 선착순 보장이 안됨

- Redis를 적용한 쿠폰 발급 흐름
  > CouponController → CouponFacade → _**CouponRedisService**_ → (Redis)   
  > (Redis) → _**CouponWorker**_ → CouponService → CouponPolicyRepository
  - 남은수량(`String`), 선착순(`ZSET`) 을 `Redis`에서 원자적으로 제어.
  - 이후 `CouponWorker`에 의해 기존 `CouponService`를 호출하여 쿠폰 발급

- 설계 의도 
  - 선착순 보장과 DB의 직접적인 부하 방지를 목적으로 했습니다.
  - `Redis`의 수량 관리를 통해 사용자에겐 즉시 응답으로 발급 중 혹은 실패를 반환하고, 
  - 확정하는 로직은 `Worker`에 의해서 동작하도록 구성했습니다.

- 책임 계층 분리
  - 사실 `CouponService` 계층을 완전히 교체 하는 방향도 고민했는데,
  - `Worker`에 의해서 실행될 때 기존의 조건부 업데이트나, 트랜젝션을 그대로 가져갈 수 있을 것으로 보여,
  - `CouponRedisService`라는 계층으로 분산 제어의 책임을 분리하도록 구성했습니다.

### Redis가 적용된 쿠폰 발급 흐름

```mermaid
sequenceDiagram
  participant Controller
  participant Facade
  participant RedisService
  participant (Redis)
  participant Worker
  participant CouponService
  participant DB

  Controller->>Facade: 발급 처리 위임
  Facade->>RedisService: 발급 처리

  RedisService->>(Redis): 잔여 수량 차감(DECR)
  alt 수량 부족
    RedisService-->>Facade: 실패(마감)
    Facade-->>Controller: 응답 반환
  else 수량 있음
    RedisService->>(Redis): 대기열 추가(PENDING ZSET)
    RedisService-->>Facade: "발급 대기"
    Facade-->>Controller: 응답 반환
  end

  Worker->>(Redis): 대기열 확인(PENDING → POP)
  Worker->>CouponService: 쿠폰 발급 확정 처리
  CouponService->>DB: 쿠폰 엔티티 저장
  Worker->>(Redis): 발급완료 처리(ISSUED SET)

```

### Worker 설계

- 기존 발급 역할을 위임 받은 CouponFacade의 동작을 대신 처리함
- 10초 미만의 시간 단위로 동작
  - 너무 길어지면 사용자에게 장시간 발급되지 않은 상태를 노출
  - 테스트 코드로 발급 쿼리 시간을 확인하여 설정 → 
- 현재 유효한 CouponPolicy ID 목록을 조회함
- policyId 별로 redis에서 PEDING 상태의 선착순 목록을 batch 사이즈(500) 만큼 가져와서 
- CouponService.issueCoupon 으로 쿠폰 발급
- 성공 시 ZSET에서 제거, ISSUED SET에 기록(중복 발급 여부 정책에 따라)
- 배치가 끝나면 발급된 수량을 Redis에 갱신(임시 차감과 실제 DB 값 동기화)

## 통합 테스트

### 통합 테스트 목적

- 동시 다발적인 요청에도 Redis에 의해 남은 수량만큼만 `PENDING`에 포함되는 지 확인한다.
- 비동기 발급 확정 검증을 위해 worker 실행 이후에 DB의 반영과 `ISSUED`로의 이동을 확인한다. 


### 조건

- [CouponRedisServiceTest.java](https://github.com/hanghae-plus-anveloper/hhplus-e-commerce-java/blob/develop/src/test/java/kr/hhplus/be/server/coupon/application/CouponRedisServiceTest.java)
- 1000개가 가용한 쿠폰에 대하여 사용자 요청은 2000개 수행
- 사용자 요청은 200개씩 10번 동시요청으로 수행(ms 값 중복 발생 테스트용)
  - 워커는 500개씩 처리시켜볼 예정이고, ms + 랜덤숫자는 3자리 숫자 001~999까지 부여할 예정입니다.

- 주요 코드

  ```java
  @Test
  @DisplayName("잔여수량이 1000개 쿠폰 정책에 대해 2000명이 동시에 발급 요청 시 Redis 응답 카운트와 DB 저장 결과를 검증한다")
  void issueCoupon_concurrently_redis() throws InterruptedException {
      /* ... */
  
      int batchSize = 200; // 200명씩 10번 나눠서 동시 요청
      for (int i = 0; i < users.size(); i += batchSize) {
          List<User> batch = users.subList(i, Math.min(i + batchSize, users.size()));
  
          CountDownLatch startLatch = new CountDownLatch(1);
          CountDownLatch doneLatch = new CountDownLatch(batch.size());
  
          for (User user : batch) {
              executor.submit(() -> {
                  try {
                      startLatch.await(); // 동시에 시작
                      boolean result = couponRedisService.tryIssue(user.getId(), policy.getId());
                      if (result) {
                          acceptedCount.incrementAndGet();
                      } else {
                          rejectedCount.incrementAndGet();
                      }
                  } catch (Exception e) {
                      rejectedCount.incrementAndGet();
                  } finally {
                      doneLatch.countDown();
                  }
              });
          }
  
          // 배치 시작
          startLatch.countDown();
          doneLatch.await(5, TimeUnit.SECONDS);
  
          System.out.println("그룹\t" + (i / batchSize + 1) + "\t완료");
      }
  
  
      start.countDown();
      done.await(10, TimeUnit.SECONDS);
      executor.shutdownNow();
  
      System.out.println("Redis 수락된 요청 수: " + acceptedCount.get());
      System.out.println("Redis 거절된 요청 수: " + rejectedCount.get());
  
      couponWorker.processPending(policy.getId());
      TimeUnit.SECONDS.sleep(1);
  
      long dbCount = couponRepository.count();
      System.out.println("DB 저장된 쿠폰 수: " + dbCount);
  
      assertThat(acceptedCount.get()).isEqualTo(availableCount);
      assertThat(dbCount).isEqualTo(availableCount);
  }
  ```

### 통합 테스트 실패 결과

![실패 - 컴파일만 성공](./assets/001-issue-coupon-fail.png)
- 동시요청 확인, 요청 결과 false 값 고정으로 컴파일만 되도록 구현

