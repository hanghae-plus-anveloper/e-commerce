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
        participant UserController
        participant BalanceService
        participant BalanceRepository
    
        Note over User,BalanceRepository: 잔액 충전 (Charge Balance)
        User->>UserController: POST /users/{userId}/balance<br>Request Body: { "amount": 1000 }
        alt userId exists and amount valid
            UserController->>BalanceService: charge(userId, amount)
            BalanceService->>BalanceRepository: findByUserId(userId)
            BalanceRepository-->>BalanceService: Balance
            BalanceService->>BalanceRepository: save(updatedBalance)
            BalanceRepository-->>BalanceService: savedBalance
            BalanceService-->>UserController: { "userId": 1, "balance": 2500 }
            UserController-->>User: 201 Created<br>Response Body: { "userId": 1, "balance": 2500 }
        else userId not found
            UserController-->>User: 404 Not Found
        else invalid amount
            UserController-->>User: 400 Bad Request (Invalid amount)
        end

        Note over User,BalanceRepository: 잔액 조회 (Get Balance)    
        User->>UserController: GET /users/{userId}/balance
        alt userId exists
            UserController->>BalanceService: getBalance(userId)
            BalanceService->>BalanceRepository: findByUserId(userId)
            BalanceRepository-->>BalanceService: Balance
            BalanceService-->>UserController: 2500
            UserController-->>User: 200 OK<br>Response Body: { "balance": 2500 }
        else userId not found
            UserController-->>User: 404 Not Found
        end
    ```
   