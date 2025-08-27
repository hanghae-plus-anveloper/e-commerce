# E-commerce Distributed Transaction 

## 학습 내용

### Saga Pattern

- `Orchestration`: `Orchestrator`가 모든 흐름을 관리
  - 순차적으로 각 도메인을 호출하고, 실패 시 보상 트랜잭션을 지시
  - 흐름이 명확하고 제어가 용이하지만, `Orchestrator`가 과도한 책임을 가지며, 확장성/유연성이 떨어질 수 있음

- `Choreography`: 각 도메인이 이벤트를 발행/구독하며 Saga를 진행
  - 도메인간 결합성이 줄어들고 확장성이 뛰어남
  - 흐름이 분산되어있어 추적/디버깅이 어려움

## 도메인 별 트랜젝션이 분리

### 목적

> 분산 환경을 고려한 도메인별 책임의 트랙젠션 로직 설계
> 현재 비즈니스 로직 중 다수의 도메인이 모두 연계되어 처리되는 상태를 단일 트랜젝션에서 관리한다면 일관성의 보장은 쉽지만, 

### 발생 가능한 문제

- 부분 실패 
  - `Order`는 생성되었지만 `Product`에서 재고 차감 실패
  - `Coupon` 사용 처리 후 `Balance` 차감 실패 → 쿠폰만 소모된 상태
- 이벤트 순서 문제
  - `StockReservedEvent`보다 `CouponUsedEvent`가 먼저 도착하거나, 중복 전달되는 경우
- 중복 처리
  - 동일 이벤트가 두 번 소비되면 잔액이 이중 차감될 위험
- 최종 일관성 지연
  - 쓰기(`Command`)는 즉시 반영되었으나, 조회(`Query`)는 이벤트 동기화 지연으로 잠시 다른 상태 보일 수 있음
- 보상 트랜잭션 필요성
  - `Balance` 차감 실패 시 → 이미 차감된 재고/쿠폰을 원복해야 함

### 설계 방향

#### 1. 주문 시작
 
- `OrderCommandService`가 사용자 요청을 받아 `Order` 엔티티를 `OrderStatus.DRAFT`로 생성
- 생성 직후 `OrderSagaRepository`에 초기 상태를 기록한 뒤 `OrderRequestedEvent` 발행
  - `OrderSagaRepository`에 저장하는 타이밍이 어려웠습니다.
  - `DRAFT` 상태를 `PENDING` 앞에 하나 더 둬서 다른 도메인에 반환 지점이 확실히 존재하도록 단계를 나눴습니다.
- 주문 상태를 `PENDING`으로 다른 도메인에 전이

#### 2. 이벤트 흐름 및 도메인 역할

- `OrderRequestedEvent(orderId, userId, items, couponId)` 발행
  - 각 도메인이 이를 구독하여 각각의 트렌젝션에서 로직 실행
  - `Product` 도메인: 재고 확인 및 차감 시도 
    - 성공 시 `StockReservedEvent(orderId)`, `PriceQuotedEvent(orderId, subtotalAmount)` 발행
    - 실패 시 `StockReserveFailedEvent(orderId, reason)` 발행
  - `Coupon` 도메인: 쿠폰이 있으면 유효성 확인 및 사용 처리 
    - 사용 시 `CouponUsedEvent(orderId, discountAmount, couponId)` 발행
    - 실패 시 `CouponUseFailedEvent(orderId, reason)` 발행
    - 미사용 시에는 `CouponSkippedEvent(orderId)` 발행

- `OrderSagaHandler`: 위 이벤트를 구독하여 Saga 상태를 갱신
  - 모든 성공 이벤트(`PriceQuoted`, `StockReservedEvent`, `CouponUsedEvent`/`CouponSkippedEvent`) 수집 
  - 결제 금액(payable) 계산 후 PaymentRequestedEvent 발행