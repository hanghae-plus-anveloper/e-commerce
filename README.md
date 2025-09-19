# E-Commerce (Java Backend)

> Java Spring Boot 기반 전자상거래 백엔드 서비스.  
> 사용자, 상품, 쿠폰, 잔액, 주문을 중심으로 도메인 주도 설계(DDD)와 계층형 아키텍처를 적용한 프로젝트입니다.   
> 동시성 제어, 이벤트 기반 처리, Redis/Kafka 활용, 성능 테스트 등 실무형 아키텍처 학습에 중점을 두었습니다.   

---

## 목차

- [E-Commerce (Java Backend)](#e-commerce-java-backend)
  - [목차](#목차)
  - [프로젝트 개요](#프로젝트-개요)
  - [아키텍처 및 설계](#아키텍처-및-설계)
    - [계층 구조 (Layered Architecture)](#계층-구조-layered-architecture)
    - [패키지 구조와 책임 분리](#패키지-구조와-책임-분리)
    - [복합 도메인 vs 단순 도메인 처리 흐름](#복합-도메인-vs-단순-도메인-처리-흐름)
  - [도메인 주요 개념](#도메인-주요-개념)
    - [User / Balance](#user--balance)
    - [Product](#product)
    - [Coupon](#coupon)
    - [Order](#order)
  - [디렉터리 구조](#디렉터리-구조)
  - [테스트 구조](#테스트-구조)
  - [환경 설정 및 실행 방법](#환경-설정-및-실행-방법)
    - [요구사항](#요구사항)
    - [실행 단계](#실행-단계)
  - [추가 문서](#추가-문서)

---

## 프로젝트 개요

E-Commerce Java 백엔드 서비스는 다음 기능들을 제공합니다:

- 사용자 조회 및 관리
- 잔액(Balance) 조회, 충전, 사용
- 상품(Product) 조회, 통계
- 쿠폰(Coupon) 발급 / 사용 / 정책 검증
- 주문(Order) 처리: 상품 재고 예약, 쿠폰 할인, 잔액 사용, 주문 완료 이벤트 발행

주요 요구사항으로는 **도메인 간 책임 분리**, **계층 간 의존성 최소화**, **복잡한 흐름(예: 주문)**의 명확한 조율(Facade 계층) 등이 있었습니다.

---

## 아키텍처 및 설계

### 계층 구조 (Layered Architecture)

```
Controller → (Facade) → Service → Domain / Repository
```

- **Controller**: HTTP 요청/응답, DTO 변환, API 명세
- **Facade**: 여러 Service를 조합해 복합 흐름(complex business logic) 처리
- **Service**: 단일 도메인의 비즈니스 로직
- **Domain / Repository**: Entity 정의, DB 접근 (JPA 기반)

### 패키지 구조와 책임 분리

각 도메인(User, Product, Coupon, Order, Balance 등)에 대해 다음과 같이 계층이 분리되어 있습니다:

- `controller`: 요청/응답 책임
- `facade`: 조율자 역할, 여러 Service 간 흐름 제어
- `application` (Service): 도메인 단위 기능
- `domain`: Entity, Repository, 상태, 내부 로직
- `exception`: 도메인/계층별 예외 정의
- `dto/common`, `common`: 공통 DTO, 에러 핸들링 등

### 복합 도메인 vs 단순 도메인 처리 흐름

| 도메인 유형     | 흐름 예시                                                                                                                                  |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| **단순 도메인** | Product → ProductController → ProductService → ProductRepository                                                                           |
| **복합 도메인** | Order 도메인: `OrderController → OrderFacade → (UserService, ProductService, CouponService, BalanceService, OrderService) → 각 Repository` |

복합 도메인의 흐름은 트랜잭션 경계 및 예외 처리 등이 복잡하므로, Facade 계층에서 책임을 중심화하여 유지보수성과 확장성을 확보하고 있음.

---

## 도메인 주요 개념

아래는 각 도메인의 핵심 역할과 오브젝트 관계/상호작용 정리입니다.

### User / Balance

- 사용자(User)는 잔액(Balance)을 가짐
- 잔액 충전 및 차감 기능
- 잔액 부족 시 예외 처리 (InsufficientBalanceException)

### Product

- 상품 정보(Product), 가격, 재고(stock)
- 상품 조회, 재고 관리
- 상품이 존재하지 않거나 재고 부족 시 예외 처리 (ProductNotFoundException 등)

### Coupon

- 쿠폰 정책(CouponPolicy) 정의
- 쿠폰 사용 유효성 체크 (기간, 소진 여부 등)
- 사용자‐쿠폰 연관 관리

### Order

- 주문 생성 흐름:

  1. 사용자 조회
  2. 상품 재고 예약 (ProductService)
  3. 주문 아이템 생성
  4. 주문 금액 합산
  5. 쿠폰 할인 계산
  6. 잔액 사용 검사 및 차감
  7. 주문 레코드 생성
  8. 주문 완료 이벤트 발행

- OrderItem, OrderStatus 등의 상태 정의
- 주문 실패 시 트랜잭션 롤백 보장됨 (예: 재고 예약 실패, 잔액 부족 등)

---

## 디렉터리 구조

아래는 주요 디렉터리 구조 (src/main/java 기준) 요약:
```
kr.hhplus.be.server
├── balance
│   ├── application / BalanceService
│   ├── controller / BalanceController, DTO
│   ├── domain / Balance, BalanceHistory, Repository
│   └── facade / BalanceFacade
├── coupon
│   ├── application / CouponService
│   ├── controller / CouponController, DTO
│   ├── domain / Coupon, Policy, Repository
│   └── facade / CouponFacade
├── order
│   ├── application / OrderService
│   ├── controller / OrderController, DTO
│   ├── domain / Order, OrderItem, Repository
│   └── facade / OrderFacade + Command/Mapper
├── product
│   ├── application / ProductService
│   ├── controller / ProductController, DTOs, Statistics API
│   ├── domain / Product, Repository
│   └── exception
├── user
│   ├── application / UserService
│   ├── controller / (UserController)
│   ├── domain / User, Repository
│   └── exception
├── common
│   ├── dto / 에러 응답 등 공통 DTO
│   └── exception / GlobalExceptionHandler 등
└── config / Swagger, JPA 등 설정
```


추가로, `docker-compose.yml`, `docker-compose-redis.yml`, `docker-compose-kafka.yml` 등이 있어 개발/로컬 환경 구성 지원됨.

---

## 테스트 구조

- 단위 테스트(Unit Tests): 각 Service, Domain 기능 별 테스트
- 통합 테스트(Integration Tests): DB 연동, Repository 동작, Facade 흐름 등이 포함됨
- 테스트 커버리지 확보가 나쁜 부분이 있다면 보강 가능

테스트 예:

- `ProductServiceTest.java`
- `OrderControllerTest.java`
- `BalanceRepositoryIntegrationTest.java` 등

---

## 환경 설정 및 실행 방법

아래는 로컬 개발 환경에서 프로젝트를 실행하기 위한 기본 안내입니다. 실제 환경에 따라 환경 변수나 설정이 다를 수 있으므로 참고용입니다.

### 요구사항

- JDK (최신 버전 권장, 예: Java 17 이상)
- Gradle (wrapper 제공됨)
- Docker & Docker Compose (Redis, Kafka 등 도커 서비스 필요 시)
- 데이터베이스 (예: MySQL, PostgreSQL 등), 혹은 내장 DB 설정

### 실행 단계

1. 코드 클론:

    ```bash
    git clone https://github.com/hanghae-plus-anveloper/e-commerce.git
    cd e-commerce
    ```

2. 환경 파일 설정:
    - `application.yml` 또는 적절한 환경 설정 파일에 DB 접속 정보, Redis/Kafka 설정 등을 입력
    - 필요 시 `.env` 파일 또는 환경 변수 사용

3. 도커 서비스 시작 (옵션):

    ```bash
    docker-compose up -d
    # 또는
    docker-compose -f docker-compose-redis.yml -f docker-compose-kafka.yml up -d    
    ```
    
4. 빌드 & 실행:
    
    ```bash
    ./gradlew clean build
    ./gradlew bootRun
    ```
    
5. API 문서 확인:
    
    Swagger 설정이 있으므로 `http://localhost:8080/swagger-ui.html` 또는 해당 엔드포인트에서 문서 확인 가능

---

## 추가 문서

- [요구사항 정의서](./docs/requirements.md)
- [플로우 차트](./docs/flow-chart/flow.md)
- [Saga 패턴](./docs/saga-pattern/design.md)
- [Redis 캐싱](./docs/cache-redis/report.md)
- [Redis 랭킹](./docs/ranking-redis/design.md)
- [Redis 비동기 처리](./docs/asynchronous-redis/design.md)
- [Kafka 비동기 처리](./docs/asynchronous-kafka/design.md)
- [랭킹 이벤트 설계](./docs/ranking-event/design.md)
- [SQL 스크립트](./docs/sql/sql.md)
