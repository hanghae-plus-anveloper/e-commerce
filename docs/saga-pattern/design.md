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

## 설계 방향

### 1. 주문 시작
 
- `OrderCommandService`가 사용자 요청을 받아 `Order` 엔티티를 `OrderStatus.DRAFT`로 생성
- 생성 직후 `OrderSagaRepository`에 초기 상태를 기록한 뒤 `OrderRequestedEvent` 발행
  - 커밋 이후에 `DRAFT` 상태로 주문을 커밋/저장하고, 이를 자체 `OrderDraftedEvent` 감지하여 Saga 저장소에 저장
    - 초기 상태를 `OrderSagaRepository`에 저장하는 타이밍이 어려웠습니다.
    - `PENDING` 앞에 상태를 하나 더 둬서 다른 도메인이 호출할 Saga 저장소가 확실히 존재하도록 단계를 나눴습니다.
- 주문 상태를 `PENDING`으로 다른 도메인에 전이

### 2. 이벤트 흐름 및 도메인 역할

- `OrderRequestedEvent(orderId, userId, items, couponId)` 발행
  - 각 도메인이 이를 구독하여 각각의 트렌젝션에서 로직 실행
  - `Product` 도메인: 재고 확인 및 차감 시도 
    - 성공 시 `StockReservedEvent(orderId)`, `PriceQuotedEvent(orderId, subtotalAmount)` 발행
    - 실패 시 `StockReserveFailedEvent(orderId, reason)` 발행
  - `Coupon` 도메인: 쿠폰이 있으면 유효성 확인 및 사용 처리 
    - 사용 시 `CouponUsedEvent(orderId, discountAmount, couponId)` 발행
    - 실패 시 `CouponUseFailedEvent(orderId, reason)` 발행
    - 미사용 시에는 `CouponSkippedEvent(orderId)` 발행 (혹은 0원으로 성공 처리)
  - `Balance` 도메인: 총액 및 차감액을 바탕으로 잔액 차감
    - 성공 이벤트(`PriceQuotedEvent`, `CouponUsedEvent`/`CouponSkippedEvent`)를 기반으로 최종 결제 금액 계산
    - 결제 가능 여부 확인 후 차감
    - 성공 시 `BalanceDeductedEvent(orderId, totalAmount)` 발행
    - 실패 시 `BalanceDeductionFailedEvent(orderId, reason)` 발행

- `OrderSagaHandler`: 위 이벤트를 구독하여 Saga 상태를 갱신
  - 모든 성공 이벤트 수집
    - `Product` → `StockReservedEvent`와 `PriceQuotedEvent` 수집
    - `Coupon` → `CouponUsedEvent` 또는 `CouponSkippedEvent` 수집
    - `Balance` → `BalanceDeductedEvent` 수집
  - 최종적으로 `OrderCompletedEvent(orderId)` 발행
    - 집계, 외부 전송 핸들러가 이를 수집 

### 3. 보상 트랜잭션 설계 - Compensation

- 각 도메인에서 실패 시 **원복 이벤트**를 발행
  - `Product`: `StockReserveFailedEvent` → 이미 차감된 재고 복구(트랜젝션에 의한 롤백), 쿠폰 사용 원복
  - `Coupon`: `CouponUseFailedEvent` → 쿠폰 사용 상태 되돌림(트랜젝션에 의한 롤백), 상품 재고 원복
  - `Balance`: `BalanceDeductionFailedEvent` → 차감된 금액 환불(트랜젝션에 의한 롤백), 쿠폰 사용 원복, 상품 재고 원복
- 각 도메인은 다른 도메인의 실패 여부를 수집하고, 실행여부를 확인하여 원복 로직 수행
- 다른 도메인의 실패 이벤트로 인한 원복 쿼리와 원래 트랜젝션 쿼리의 충돌이 발생할 수도 있고,
- 실패 이벤트가 먼저 수집된 뒤에 정상 이벤트를 수집하게 되면 실패가 무시되어 실행될 수 있음
- Saga 저장소에서 도메인별 성공/실패/취소됨/복구됨 상태를 관리

### 4. CQRS 설계

- 각 도메인 별 Command, Query 모델을 분리하여 수정과 조회 로직을 분산
- 롤백이 필요한 수정 함수만 트랜젝션 내에서 처리 가능
- 다만, 지금 모든 도메인이 각자 상태값을 바꾸는 설계로 구현하여 현재 주문 생성 중에는 각 도메인의 `Command` 부분만 사용

### 5. Saga 상태 저장소

- 발급된 Saga의 현재 상태를 추적하기 위한 저장소, JPA 혹은 Redis에 구현 가능
- 모든 도메인이 이벤트 기반으로 움직이기 때문에, 영속성을 강하게 가져가기 위해 JPA 기반으로 구현

- `OrderSagaState`: 필요 상태 정보
  - 주문 메타 정보 
    - `orderId`: 최초 DRAFT의 상태의 주문 정보 PK
    - `userId`: 사용자 ID
    - `items`: 사용자가 요청한 item 배열
      - `productId`: 상품 번호
      - `quantity`: 상품 수량
    - `couponId`: 쿠폰 사용 시 쿠폰 번호
  - 각 도메인 별 트랜젝션 수행 정보: `SUCCESS`, `FAILED`, `CANCELED`, `RESTORED`
    - `productReserved`: 상품 재고 차감 수행, 총액 반환 여부
    - `couponApplied`: 쿠폰 사용 여부
    - `balanceCharged`: 잔액 차감 여부
  - 잔액 계산을 위한 결과값 정보 / 이벤트에 담아서도 전송하지만, 소실되어 이벤트를 재발행하거나, 원복해야할 내용 참조를 위해
    - `subTotalAmount`: 상품 총액 원가
    - `discountAmount`: 쿠폰 할인액
    - `totalAmount`: 최종 금액

## 테스트 및 기능 구현

### 상태 저장소 구성

- `OrderSagaState`: 주문 Saga 상태값
- `OrderSagaRepository`: Saga 상태 저장소
- `OrderSagaEventStatus(enum: PENDING, SUCCESS, FAILED, CANCELED, RESTORED)`

### Saga 상태 머신 핸들러

- `OrderSagaHandler`: Saga의 상태 관리 메타 데이터 및 부가 정보 관리

### 도메인 이벤트

- Order
  - `OrderDraftedEvent`: 초안 생성됨 이벤트, 자기 참조용
  - `OrderRequestedEvent`: 주문 요청됨 이벤트, 각 도메인 참조용
  - `OrderCompletedEvent`: 주문 완료 이벤트
  - `OrderFailedEvent`: 주문 실패 이벤트
- Product
  - `StockReservedEvent`: 재고 차감 완료 이벤트
  - `PriceQuotedEvent`: 주문 총액 이벤트(재고 차감 이벤트와 통합 가능)
  - `StockReserveFailedEvent`: 재고 차감 실패 이벤트 
- Coupon
  - `CouponUsedEvent`: 쿠폰 사용 이벤트 + 할인액 반환
  - `CouponSkippedEvent`: 쿠폰 미사용 이벤트(쿠폰 0원 사용 이벤트로 도 가능 할 것으로 보임)
  - `CouponUseFailedEvent`: 쿠폰 사용 실패 이벤트
- Balance
  - `BalanceDeductedEvent`: 잔액 차감 이벤트
  - `BalanceDeductionFailedEvent`: 잔액 차감 실패 이벤트

### 주문 생성 주체

- `OrderCommandService`
- `OrderEventHandler`

### 각 도메인별 핸들러 및 테스트

- Product
  - `ProductCommandService`: 상품 재고 차감/원복 비즈니스 로직
  - `ProductEventHandler`: 상품 차감 및 원가 반환
  - `ProductCompensationHandler`: 차감된 상품 재고 보상
- Coupon
  - `CouponCommandService`: 쿠폰 사용/원복 비즈니스 로직
  - `CouponEventHandler`: 쿠폰 사용 및 할인액 반환
  - `CouponCompensationHandler`: 사용된 쿠폰 원복 보상
- Balance
  - `BalanceCommandService`: 잔액 차감/원복 비즈니스 로직 
  - `BalanceEventHandler`: 최종 금액 차감
  - `BalanceCompensationHandler`: 잔액 원복
    - 주문이 실패하는 경우에 원복인데, 주문이 이미 만들어진 상태이고, 
    - 각각의 도메인이 먼저 수행된 뒤에 동작이기 때문에 불필요할 것으로 보임  


### 기타 집계 및 외부 호출 Mock 핸들러

- `TopProductEventHandler` (구현됨)
- `OrderExternalEventHandler` (구현됨)

