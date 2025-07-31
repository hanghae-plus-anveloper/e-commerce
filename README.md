# 프로젝트 아키텍처 설명

이 프로젝트는 Layered Architecture를 기반으로 설계되었으며, 도메인별 기능을 중심으로 디렉토리와 책임을 나누어 구성되어 있습니다.

## 전체 계층 구조

```
Controller → (Facade) → Service → Domain/Repository
```

-   일반 기능: `Controller → Service → Repository`
-   복합 로직(`User`, `Order`): `Controller → Facade → 각 Service → 각 Repository`

## 디렉토리 구성 및 역할

### 1. controller

클라이언트로부터의 HTTP 요청을 받아 처리하는 계층입니다.
요청 검증 및 DTO 변환, 응답 생성의 책임을 가집니다.

-   `*Controller.java`: HTTP 요청 핸들러 클래스
-   `*Api.java`: API 명세용 인터페이스
-   `*Dto.java`: 요청(Request)/응답(Response) DTO 정의

> 하위 디렉토리: `coupon`, `order`, `product`, `user`

### 2. facade

여러 Service 간의 흐름을 조율하는 계층입니다.
복잡한 트랜잭션 처리나 도메인 간 협업이 필요한 경우 이 계층을 통해 로직을 단순화합니다.

-   `UserFacade`: `UserService`, `BalanceService` 조합
-   `CouponFacade`: `UserService`, `CouponService` 조합
-   `OrderFacade`: `UserService`, `OrderService`, `BalanceService`, `CouponService`, `ProductService` 조합

### 3. application

비즈니스 로직을 담당하는 계층입니다.
단일 책임 원칙에 따라 기능별 Service로 나누어져 있으며,
일부 복잡한 도메인(User, Order)은 별도의 Facade를 통해 여러 Service를 조합합니다.

-   `*Service.java`: 각 도메인 단위의 비즈니스 처리 로직
-   `user`, `order`, `coupon`: 복합 흐름을 다루기 위해 `facade` 계층 사용

> 하위 디렉토리: `balance`, `coupon`, `order`, `product`, `user`

### 4. domain

실제 데이터베이스와 연결되는 계층입니다.
JPA 기반의 Entity, Repository로 구성되어 있으며 각 도메인은 하위 디렉토리로 구분됩니다.

-   `Entity`: `@Entity`로 선언된 JPA 객체
-   `Repository`: `JpaRepository` 상속 인터페이스

> 하위 도메인: `balance`, `coupon`, `order`, `product`, `user`

### 5. dto/common

공통적으로 사용하는 DTO가 위치합니다. 주로 에러 응답 등의 형식을 정의합니다.

-   `CustomErrorResponse.java`: 표준 에러 응답 포맷

### 6. exception

예외 처리를 담당하는 계층입니다. 사용자 정의 예외, 글로벌 예외 핸들러 등을 포함합니다.

### 7. infrastructure/external - 설계 기획, 미구현

카카오, Slack 알림과 같이 외부 연동을 위한 Service가 위치합니다.

### 디렉토리 구조

```bash
npx --yes file-tree-cli src/main/java --ext java
npx --yes file-tree-cli src/test/java --ext java
```

```
## npx --yes file-tree-cli src/main/java --ext java
/Workspace/hhplus-e-commerce-java/src/main/java
└── kr
    └── hhplus
        └── be
            └── server
                ├── ServerApplication.java
                ├── balance
                │   ├── application
                │   │   └── BalanceService.java
                │   ├── controller
                │   │   ├── BalanceApi.java
                │   │   ├── BalanceController.java
                │   │   ├── BalanceResponseDto.java
                │   │   └── ChargeRequestDto.java
                │   ├── domain
                │   │   ├── Balance.java
                │   │   ├── BalanceChangeType.java
                │   │   ├── BalanceHistory.java
                │   │   ├── BalanceHistoryRepository.java
                │   │   └── BalanceRepository.java
                │   ├── exception
                │   │   └── InsufficientBalanceException.java
                │   └── facade
                │       └── BalanceFacade.java
                ├── common
                │   ├── dto
                │   │   └── CustomErrorResponse.java
                │   └── exception
                │       └── GlobalExceptionHandler.java
                ├── config
                │   ├── jpa
                │   │   └── JpaConfig.java
                │   └── swagger
                │       └── SwaggerConfig.java
                ├── coupon
                │   ├── application
                │   │   └── CouponService.java
                │   ├── controller
                │   │   ├── CouponApi.java
                │   │   ├── CouponController.java
                │   │   └── CouponResponseDto.java
                │   ├── domain
                │   │   ├── Coupon.java
                │   │   ├── CouponPolicy.java
                │   │   ├── CouponPolicyRepository.java
                │   │   └── CouponRepository.java
                │   ├── exception
                │   │   ├── CouponSoldOutException.java
                │   │   └── InvalidCouponException.java
                │   └── facade
                │       └── CouponFacade.java
                ├── order
                │   ├── application
                │   │   └── OrderService.java
                │   ├── controller
                │   │   ├── OrderApi.java
                │   │   ├── OrderController.java
                │   │   ├── OrderItemRequestDto.java
                │   │   ├── OrderRequestDto.java
                │   │   └── OrderResponseDto.java
                │   ├── domain
                │   │   ├── Order.java
                │   │   ├── OrderItem.java
                │   │   ├── OrderItemRepository.java
                │   │   ├── OrderRepository.java
                │   │   └── OrderStatus.java
                │   └── facade
                │       ├── OrderCommandMapper.java
                │       ├── OrderFacade.java
                │       └── OrderItemCommand.java
                ├── product
                │   ├── application
                │   │   └── ProductService.java
                │   ├── controller
                │   │   ├── ProductApi.java
                │   │   ├── ProductController.java
                │   │   ├── ProductResponseDto.java
                │   │   ├── ProductStatisticsApi.java
                │   │   ├── ProductStatisticsController.java
                │   │   └── TopProductResponseDto.java
                │   ├── domain
                │   │   ├── Product.java
                │   │   └── ProductRepository.java
                │   └── exception
                │       └── ProductNotFoundException.java
                └── user
                    ├── application
                    │   └── UserService.java
                    ├── domain
                    │   ├── User.java
                    │   └── UserRepository.java
                    └── exception
                        └── UserNotFoundException.java


## npx --yes file-tree-cli src/test/java --ext java
/Workspace/hhplus-e-commerce-java/src/test/java
└── kr
    └── hhplus
        └── be
            └── server
                ├── ServerApplicationTests.java
                ├── TestcontainersConfiguration.java
                ├── balance
                │   ├── application
                │   │   └── BalanceServiceTest.java
                │   ├── controller
                │   │   └── BalanceControllerTest.java
                │   ├── domain
                │   │   ├── BalanceRepositoryIntegrationTest.java
                │   │   ├── BalanceRepositoryTest.java
                │   │   └── BalanceTest.java
                │   └── facede
                │       └── BalanceFacadeTest.java
                ├── coupon
                │   ├── application
                │   │   └── CouponServiceTest.java
                │   ├── controller
                │   │   └── CouponControllerTest.java
                │   ├── domain
                │   │   ├── CouponPolicyTest.java
                │   │   ├── CouponRepositoryIntegrationTest.java
                │   │   ├── CouponRepositoryTest.java
                │   │   └── CouponTest.java
                │   └── facade
                │       └── CouponFacadeTest.java
                ├── order
                │   ├── application
                │   │   └── OrderServiceTest.java
                │   ├── controller
                │   │   └── OrderControllerTest.java
                │   └── domain
                │       ├── OrderItemTest.java
                │       ├── OrderRepositoryIntegrationTest.java
                │       ├── OrderRepositoryTest.java
                │       └── OrderTest.java
                ├── product
                │   ├── application
                │   │   └── ProductServiceTest.java
                │   ├── controller
                │   │   └── ProductControllerTest.java
                │   └── domain
                │       ├── ProductRepositoryIntegrationTest.java
                │       ├── ProductRepositoryTest.java
                │       └── ProductTest.java
                └── user
                    ├── application
                    │   └── UserServiceTest.java
                    └── domain
                        ├── UserRepositoryIntegrationTest.java
                        ├── UserRepositoryTest.java
                        └── UserTest.java


```

## 레이어 간 흐름 예시

### 1. 단순한 구조 - `Product`

```
ProductController
  → ProductService
    → ProductRepository
```

### 2. 복합 구조 - `User`

```
UserController
  → UserFacade
    → UserService
      → UserRepository
    → BalanceService
      → BalanceRepository
```

### 3. 복합 구조 - `Coupon`

```
CouponController
  → CouponFacade
    → UserService
      → UserRepository
    → CouponService
      → CouponRepository
```


### 4. 복합 구조 - `Order`

```
OrderController
  → OrderFacade
    → UserService
      → UserRepository
    → ProductService
      → ProductRepository
    → CouponService
      → CouponRepository
    → BalanceService
      → BalanceRepository
    → OrderService
      → OrderRepository
    → (NotificationService) ## 외부 예시 - 미구현
```

## 설계 의도 요약

-   **레이어드별/도메인별 패키지 구성**: 각 도메인이 담당하는 책임이 명확하게 분리
-   **단순 도메인과 복합 도메인의 처리 방식 분리**: 유지보수성과 테스트 용이성, 기능 확장을 고려하여 `Facade` 도입
-   **계층 간 의존성 최소화**: `Controller`는 `Service` 또는 `Facade`에만 의존하며, 내부 도메인에 대한 직접 접근은 하지 않음

<!-- ---

#### Running Docker Containers

`local` profile 로 실행하기 위하여 인프라가 설정되어 있는 Docker 컨테이너를 실행해주셔야 합니다.

```bash
docker-compose up -d
``` -->
