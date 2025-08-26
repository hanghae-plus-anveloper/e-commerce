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

### 설계 방향

- `OrderFacade`에서 주문에 대한 기본 정보를 조회
  - READ: 재고 및 금액 확인, 유요한 쿠폰 여부 확인, 잔액이 충분한지 확인은 Facade 내에서 각 서비스에 직접 조회 
  - UPDATE: 재고 차감, 쿠폰 사용, 잔액 차감은 분리된 도메인에서 발행된 이벤트를 확인하여 수행

- 단순 조회나 가능 여부는 각각의 도메인의 트랜젝션 없이(롤백이 없으니) 기존 `OrderFacade`에서 수행
  - Order의 status를 추가하고, `PENDING` 상태로 생성 후 주문 생성 이벤트`OrderPlacedEvent` 발행
  - OrderPlacedEvent
    - orderId: Saga 테이블 구분자
    - items: 재고 차감용 상품 id 및 수량 배열
    - couponId: 사용 처리할 쿠폰 id
    - totalPrice: 잔액 차감용 최종 금액(쿠폰 할인 예상액 포함)
  - `OrderEventHandler` 자체에도 `OrderPlacedEvent`감지하여 Saga 테이블(Redis 기반으로 예정) 상에 orderId 기반으로 hashes에 저장 
- 각각의 도메인에서 차감/사용 등 UPDATE를 개별 트랜젝션 내에서 처리 후 성공 실패 여부를 이벤트로 발행
  - `OrderEventHandler`가 각 도메인이 발행하는 성공/실패 이벤트를 받아서 이후 로직 처리
  - 모두 반환되었을때엔 PENDING에서 COMPLETED로 변경하고
  - 한가지라도 실패되었을 때, 나머지 도메인에서 orderId기반으로 보상 트랜젝션이 동작하도록 추가 로직 수행