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
