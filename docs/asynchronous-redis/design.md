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
    - `DECR`로 원자적 차감
    - `expire` 설정
      - `CouponPolicy`의 `endedAt`로 설정
      - 발급 후에 차감되는 값이며, DB의 정보로 갱신

- 선착순 관리
  - key: `COUPON:POLICY:{policyId}:PENDING`
  - 자료구조: `ZSET`(`userId`, `timestamp`)
    - Redis 선착순 통과 직후 사용자를 넣음
    - score: 요청 시간 저장 → "누가 몇 번째로 도착했는지" 순서 관리 가능
      - `ms`까지 동일한 경우를 대비해서, `ms + 랜덤 숫자`
        - ms까지 동일한데, 1장 남았을 경우가 과연 많을까 고민하였지만,  
    - consumer가 DB 반영 후 ISSUED로 이동.

- 발급 확정
  - key: `COUPON:POLICY:{policyId}:ISSUED`
  - 자료구조: `SET`(`userId`)
    - DB 반영 완료된 사용자
    - 중복 요청 차단 및 발급 최종 이력 확인용

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

## 통합 테스트

