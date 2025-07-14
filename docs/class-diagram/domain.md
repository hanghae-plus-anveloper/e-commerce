1. Class Diagram

- `User`, `Balance`, `Coupon`, `Order`, `Product` 관련 객체 중심 클래스 다이어그램 작성

```mermaid
classDiagram
    class User {
        +Long id
        +String name
    }

    class Balance {
        +Long userId
        +int balance
    }

    class BalanceHistory {
        +Long id
        +Long balanceId
        +int amount
        +String type  %% "CHARGE" | "DEDUCT"
        +Date timestamp
    }

    class Coupon {
        +Long id
        +int discountAmount
        +boolean used
    }

    class Order {
        +Long id
        +int totalAmount
    }

    class OrderItem {
        +Long productId
        +int quantity
    }

    class Product {
        +Long id
        +String name
    }

    class Option {
        +String code
        +Long productId
        +int price
        +int stock
    }

    class TopProductSummary {
        +Long productId
        +int totalSold
    }

    User "1" --> "1" Balance : 잔액
    Balance "1" --> "*" BalanceHistory : 이력
    User "1" --> "*" Coupon : 보유
    User "1" --> "*" Order : 주문
    Order "1" --> "*" OrderItem : 포함
    OrderItem "1" --> "1" Option : 대상
    Product "1" --> "*" Option : 상품옵션
```