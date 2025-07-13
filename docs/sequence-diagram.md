### Sequence Diagram 

1. `주요` 잔액 충전 / 조회 API

    > - 결제에 사용될 금액을 충전하는 API 를 작성합니다.
    > - 사용자 식별자 및 충전할 금액을 받아 잔액을 충전합니다.
    > - 사용자 식별자를 통해 해당 사용자의 잔액을 조회합니다.

    - 시퀀스 설명
        - 사용자가 잔액을 충전하거나 잔액을 조회합니다.
        - 충전 요청 시 기존 잔액을 갱신하고, 조회 시 현재 금액을 반환합니다.

    - 잔액 충전 / 조회 API 시퀀스

    ```mermaid
    sequenceDiagram
        title 잔액 충전 / 조회 API 시퀀스
        participant User
        participant BalanceController
        participant BalanceService
        participant BalanceRepository
    
        Note over User,BalanceRepository: 잔액 충전 (Charge Balance)
        User->>BalanceController: POST /users/{userId}/balance<br>Request Body: { "amount": 1000 }
        alt userId exists and amount valid
            BalanceController->>BalanceService: charge(userId, amount)
            BalanceService->>BalanceRepository: findByUserId(userId)
            BalanceRepository-->>BalanceService: Balance
            BalanceService->>BalanceRepository: save(updatedBalance)
            BalanceRepository-->>BalanceService: savedBalance
            BalanceService-->>BalanceController: { "userId": 1, "balance": 2500 }
            BalanceController-->>User: 201 Created<br>Response Body: { "userId": 1, "balance": 2500 }
        else userId not found
            BalanceController-->>User: 404 Not Found
        else invalid amount
            BalanceController-->>User: 400 Bad Request (Invalid amount)
        end

        Note over User,BalanceRepository: 잔액 조회 (Get Balance)    
        User->>BalanceController: GET /users/{userId}/balance
        alt userId exists
            BalanceController->>BalanceService: getBalance(userId)
            BalanceService->>BalanceRepository: findByUserId(userId)
            BalanceRepository-->>BalanceService: Balance
            BalanceService-->>BalanceController: 2500
            BalanceController-->>User: 200 OK<br>Response Body: { "balance": 2500 }
        else userId not found
            BalanceController-->>User: 404 Not Found
        end
    ```
   
2. `기본` 상품 조회 API

    > - 상품 정보 ( ID, 이름, 가격, 잔여수량 ) 을 조회하는 API 를 작성합니다.
    > - 조회시점의 상품별 잔여수량이 정확하면 좋습니다.

    - 시퀀스 설명
        - 전체 상품 목록 또는 특정 상품을 조회합니다.
        - 각 상품의 재고 정보를 함께 제공해야 하며, 정확한 수량이 보장되어야 합니다.

    - 상품 조회 API 시퀀스

    ```mermaid
    sequenceDiagram
        title 상품 조회 API 시퀀스
        participant User
        participant ProductController
        participant ProductService
        participant ProductRepository

        Note over User,ProductRepository: 전체 상품 목록 조회
        User->>ProductController: GET /products
        ProductController->>ProductService: getAllProducts()
        ProductService->>ProductRepository: findAllWithStock()
        ProductRepository-->>ProductService: List<Product>
        ProductService-->>ProductController: List<ProductDTO>
        ProductController-->>User: 200 OK<br>Response Body: [ { id, name, price, stock }, ... ]
   
        Note over User,ProductRepository: 단일 상품 조회
        User->>ProductController: GET /products/{productId}
        ProductController->>ProductService: getProduct(productId)
        ProductService->>ProductRepository: findById(productId)
        alt product exists
            ProductRepository-->>ProductService: Product
            ProductService-->>ProductController: ProductDTO
            ProductController-->>User: 200 OK<br>Response Body: { id, name, price, stock }
        else not found
            ProductRepository-->>ProductService: null
            ProductService-->>ProductController: NotFound
            ProductController-->>User: 404 Not Found
        end
    ```

3. `주요` 선착순 쿠폰 기능

    > - 선착순 쿠폰 발급 API 및 보유 쿠폰 목록 조회 API 를 작성합니다.
    > - 사용자는 선착순으로 할인 쿠폰을 발급받을 수 있습니다.
    > - 주문 시에 유효한 할인 쿠폰을 함께 제출하면, 전체 주문금액에 대해 할인 혜택을 부여받을 수 있습니다.

    - 시퀀스 설명
        - 사용자는 선착순으로 할인 쿠폰을 발급받을 수 있습니다.
        - 이미 모두 소진된 경우 발급은 제한되며, 보유 쿠폰 목록도 확인할 수 있습니다.

    - 선착순 쿠폰 발급 API 시퀀스
    
    ```mermaid
    sequenceDiagram
        title 선착순 쿠폰 발급 API 시퀀스
        participant User
        participant CouponController
        participant CouponService
        participant CouponRepository

        Note over User,CouponRepository: 쿠폰 발급 요청 (Coupon)
        User->>CouponController: POST /coupons
        CouponController->>CouponService: claimCoupon(userId)
        CouponService->>CouponRepository: countIssuedCoupons()
        alt coupons exists
            CouponService->>CouponRepository: save(userId)
            CouponRepository-->>CouponService: Coupon
            CouponService-->>CouponController: Coupon
            CouponController-->>User: 201 Created<br>Response Body: { "couponId": 1, "discount": 1000 }
        else coupons sold out
            CouponService-->>CouponController: throw CouponSoldOutException
            CouponController-->>User: 409 Conflict<br>Message: "Coupons sold out"
        end
    ```
   
    - 사용자 보유 쿠폰 조회 API 시퀀스
   
    ```mermaid
    sequenceDiagram
        title 사용자 보유 쿠폰 조회 API 시퀀스
        participant User
        participant CouponController
        participant CouponService
        participant CouponRepository
    
        Note over User,CouponRepository: 보유 쿠폰 목록 조회 (List My Coupons)
        User->>CouponController: GET /users/{userId}/coupons
        CouponController->>CouponService: getCouponsByUserId(userId)
        CouponService->>CouponRepository: findByUserId(userId)
        CouponRepository-->>CouponService: List<Coupon>
        CouponService-->>CouponController: List<Coupon>
        CouponController-->>User: 200 OK<br>Response Body: [ { "couponId": 1, ... }, ... ]
    ```
   
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
        OrderController->>OrderService: placeOrder(userId, items, couponId)
    
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

5. `기본` 상위 상품 조회 API

    > - 최근 3일간 가장 많이 팔린 상위 5개 상품 정보를 제공하는 API 를 작성합니다.
    > - 통계 정보를 다루기 위한 기술적 고민을 충분히 해보도록 합니다.

    - 시퀀스 설명
        - 최근 3일간 판매량을 기준으로 상위 5개 인기 상품을 집계합니다.
        - 주문 데이터를 기반으로 통계를 계산하고 상품 정보를 함께 반환합니다.

    - 최근 가장 많이 팔린 상품 직접 조회 API 시퀀스

    ```mermaid
    sequenceDiagram
        title 상위 상품 조회 API 시퀀스
        participant User
        participant ProductController
        participant ProductStatisticsService
        participant OrderRepository
        participant ProductRepository

        Note over User,ProductStatisticsService: 상위 5개 인기 상품 조회

        User->>ProductController: GET /products/top
        ProductController->>ProductStatisticsService: getTopSellingProducts(last3days, limit=5)
        ProductStatisticsService->>OrderRepository: findOrders(last3days)
        OrderRepository-->>ProductStatisticsService: [OrderItems]
        ProductStatisticsService->>ProductRepository: findProductsByIds(top5ProductIds)
        ProductRepository-->>ProductStatisticsService: [Product]
        ProductStatisticsService-->>ProductAPI: [Top5ProductResponse]
        ProductController-->>User: 200 OK<br>Response Body: [ { id, name, price, stock }, ... ]
    ```

    - 사전 집계 기반 상위 상품 조회 API 시퀀스

    ```mermaid
    sequenceDiagram
        title 사전 집계 기반 상위 상품 조회 API 시퀀스
        participant User
        participant ProductController
        participant ProductStatisticsService
        participant ProductStatisticsRepository
        participant ProductRepository
   
        Note over User,ProductStatisticsService: 사전 집계된 인기 상품 정보 조회
   
        User->>ProductController: GET /products/top
        ProductController->>ProductStatisticsService: getTopSellingProducts(limit=5)
        ProductStatisticsService->>ProductStatisticsRepository: findTopProducts(last3days, limit=5)
        ProductStatisticsRepository-->>ProductStatisticsService: [TopProductSummary]
        ProductStatisticsService->>ProductRepository: findProductsByIds([productIds])
        ProductRepository-->>ProductStatisticsService: [Product]
        ProductStatisticsService-->>ProductController: [Top5ProductResponse]
        ProductController-->>User: 200 OK<br>Response Body: [ { id, name, price, stock }, ... ] 
    ```
