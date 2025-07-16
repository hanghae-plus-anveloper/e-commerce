4. `주요` 주문 / 결제 API

   > - 사용자 식별자와 (상품 ID, 수량) 목록을 입력받아 주문하고 결제를 수행하는 API 를 작성합니다.
   > - 결제는 기 충전된 잔액을 기반으로 수행하며 성공할 시 잔액을 차감해야 합니다.
   > - 데이터 분석을 위해 결제 성공 시에 실시간으로 주문 정보를 데이터 플랫폼에 전송해야 합니다. ( 데이터 플랫폼이 어플리케이션 `외부` 라는 가정만 지켜 작업해 주시면 됩니다 )

   - 시퀀스 설명

     - 사용자는 상품 목록을 선택해 주문하고, 충전된 잔액으로 결제합니다.
     - 결제 성공 시 주문 정보를 저장하고, 외부 데이터 플랫폼에 전송합니다.

   - 주문 / 결제 API 시퀀스

   ```mermaid
   sequenceDiagram
       title 주문 / 결제 API 시퀀스
       participant User
       participant OrderController
       participant OrderService
       participant BalanceService
       participant ProductService
       participant CouponService
       participant OrderRepository
       participant ExternalPlatformClient

       Note over User,OrderService: 주문 생성 및 결제 처리 (Place Order & Pay)
       User->>OrderController: POST /orders<br>Request Body: { userId, items: [{productId, quantity}], couponId? }
       OrderController->>OrderService: createOrder(userId, items, couponId)

       alt 유저 존재하지 않음
           OrderService-->>OrderController: throw UserNotFoundException
           OrderController-->>User: 404 Not Found
       end

       Note over OrderService,BalanceService: 잔액 확인
       OrderService->>BalanceService: verifyAndDeductBalance(userId, totalAmount)
       alt 잔액 부족
           BalanceService-->>OrderService: throw InsufficientBalanceException
           OrderService-->>OrderController: 400 Bad Request
           OrderController-->>User: 400 Bad Request (잔액 부족)
       end


       Note over OrderService,ProductService: 상품별 재고 확인
       loop for each item
           OrderService->>ProductService: verifyAndDeductStock(productId, quantity)
           alt 재고 부족
               ProductService-->>OrderService: throw OutOfStockException
               OrderService-->>OrderController: 400 Bad Request
               OrderController-->>User: 400 Bad Request (재고 부족)
           end
       end

       Note over OrderService,CouponService: 쿠폰 사용시, 쿠폰 유효성 확인
       alt 쿠폰 사용 포함
           OrderService->>CouponService: applyCoupon(userId, couponId)
           alt 유효하지 않음
               CouponService-->>OrderService: throw InvalidCouponException
               OrderService-->>OrderController: 400 Bad Request
               OrderController-->>User: 400 Bad Request (쿠폰 오류)
           end
       end

       OrderService->>OrderRepository: save(order)
       OrderRepository-->>OrderService: savedOrder

       Note over OrderService,ExternalPlatformClient: 주문 정보 외부 전송
       OrderService->>ExternalPlatformClient: sendOrder(savedOrder)

       OrderService-->>OrderController: savedOrder
       OrderController-->>User: 201 Created<br>Response Body: { orderId, ... }

   ```
