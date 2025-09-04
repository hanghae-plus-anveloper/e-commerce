# E-commerce Asynchronous Design With Kafka

> 기존 `Redis`는 선착순을 메모리 기반으로 빠르게 보장하고, DB의 영속화는 Kafka를 통해 병렬적으로 처리

## Redis 기반 선착순 쿠폰 발급 설계

### 기존 설계

> - 지지난주차에 과제 피드백을 반영하여 수정되었습니다.
>   - `RedisService`는 부적절, `RedisRepository`인프라 레이어로 `service`에서 사용

- `Client` → `Redis` 발급 시도 및 즉시 응답
  ```mermaid
  sequenceDiagram
    Client->>CouponService: tryIssue(userId, policyId)
    CouponService->>CouponRedisRepository: tryIssue(userId, policyId)
    CouponRedisRepository->>Redis: DECR & ZADD(PENDING)
    CouponRedisRepository-->>CouponService: true/false
    CouponService-->>Client: OK/Fail
  ```
  - DECR로 남은 수량을 원자적으로 차감(선착순 게이트).
  - 성공 시 PENDING ZSET에 (userId, 시간+랜덤)으로 대기열 등록.
  - 호출자는 즉시 성공/실패를 응답받음(실제 DB 확정은 별도 단계).

- `Worker`에 의한 DB ↔ `Redis` 정책 동기화
  ```mermaid
  sequenceDiagram
    CouponWorker->>CouponService: syncActivePolicies()
    CouponService->>CouponPolicyRepository: findActive()
    CouponService->>CouponRedisRepository: setRemaining/removePolicy
  ```
  - DB의 유효 정책과 Redis의 잔여수량 키를 맞춤.
  - `Redis`에만 남은 오래된 정책 키는 정리.
  - 활성 정책의 `remainingCount`를 `Redis`에 동기화.

- `Worker`에 의한 대기열 처리
  ```mermaid
  sequenceDiagram
    CouponWorker->>CouponService: processAllPending()
    CouponService->>CouponRedisRepository: peekPending(N)
    loop for each userId
      CouponService->>CouponService: issueCoupon(userId, policyId)
      CouponService->>CouponPolicyRepository: decreaseRemaining()
      CouponService->>CouponRepository: save(Coupon)
    end
    CouponService->>CouponRedisRepository: removePending(succeeded)
  ```
  - 1초마다 PENDING ZSET을 배치(최대 500) 로 가져와 DB에 발급 확정.
  - 성공한 사용자만 ZREM으로 PENDING에서 제거.
  - PENDING이 비면 키를 삭제(수동 만료 전략).
  - DB는 트랜잭션 & 조건부 감소로 초과 발급 방지(동시성 예외는 무시/스킵).

### 변경된 설계

- `Client` → `Redis` 발급 시도 및 즉시 응답
  ```mermaid
  sequenceDiagram
    Client->>CouponService: tryIssue(userId, policyId)
    CouponService->>CouponRedisRepository: tryIssue(userId, policyId)
    CouponRedisRepository->>Redis: DECR & ZADD(PENDING)
    alt 성공
      CouponService-->>Client: Accepted
      CouponService->>CouponEventPublisher: publish(CouponIssueRequestedEvent)
    else 실패
      CouponService-->>Client: SoldOut
    end
  ```
  - Redis 선착순 성공 직후 이벤트 발행
    - 동기 로직과 분리
    - RedisRepository는 Transaction이 아니므로, 즉시 이벤트 발행

- `Kafka` 컨슈머에 의한 DB 확정 처리
  ```mermaid
  sequenceDiagram
    Kafka->>CouponIssueConsumer: CouponIssueRequestedMessage(userId, policyId)
    CouponIssueConsumer->>CouponService: issueCoupon(userId, policyId)
    CouponService->>CouponPolicyRepository: decreaseRemaining(policyId)
    CouponService->>CouponRepository: save(Coupon)
    CouponIssueConsumer-->>Kafka: commit offset
  ```
  - Kafka Consumer에 의해서 CouponService 쿠폰 발급
    - 학습 목적 상 분산 환경, 혹은 다른 서비스로 분리되었다 가정
  - 성공시 offset 커밋하여 순차 진행

- `Worker`에 의한 DB ↔ `Redis` 정책 동기화 **(동일)**
  ```mermaid
  sequenceDiagram
    CouponWorker->>CouponService: syncActivePolicies()
    CouponService->>CouponPolicyRepository: findActive()
    CouponService->>CouponRedisRepository: setRemaining/removePolicy
  ```
  - `RedisRepository`의 상태를 주기적으로 동기화 하는 역할만 수행

##