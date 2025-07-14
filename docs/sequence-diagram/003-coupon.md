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
   