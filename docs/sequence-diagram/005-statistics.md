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
        ProductController-->>User: 200 OK<br>Response Body: [ { id, name, price, stock, soldCount }, ... ]
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
        ProductController-->>User: 200 OK<br>Response Body: [ { id, name, price, stock, soldCount }, ... ] 
    ```
