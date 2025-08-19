# E-commerce Ranking Design

> `Redis` 기반의 가장 많이 주문한 상품 랭킹을 설계한다.
> 해당 설계를 기반으로 개발 및 구현한다.

## Redis 기반 상품 랭킹 설계

### 목표

- 기존 DB(NativeQuery) + 캐싱 기반의 상품 랭킹에서, Redis 기반 집계 방식으로 전환
- 트래픽 급증 상황에서도 빠른 랭킹 조회 성능 확보, 
- 조회 조건: 오늘을 포함한 최근 3일 간 랭킹

### 기존 방식

- **DB 쿼리 기반 + 캐싱**
  - 사용자 조회 시 캐시 확인 → 없으면 DB Native Query 실행
  - 실행 결과를 Redis 캐시에 저장 (자정 만료)
- 장점: DB 데이터 정합성 100% 보장
- 단점: 조회 시점마다 DB 부하 발생 → 트래픽 급증 시 성능 저하

### 개선 방식 (Redis 기반 집계)

- 주문 발생 시점에 Redis에 바로 집계 반영
  - 주문 생성 시 Redis Sorted Set(`ZSET`)에 `ZINCRBY`로 상품 판매량 업데이트
- 랭킹 조회 시점
  - Redis Sorted Set(ZSET)에서 누적된 값 바로 활용
  - 최근 3일 집계: `ZUNIONSTORE` 또는 `ZUNION` 연산으로 3일치 키 병합
- 정합성 보장
  - Redis 업데이트는 트랜잭션 커밋 이후에만 수행 ← `차주 추가 구현, 일단 기능 수행`
  - ~~이벤트 트리거는 `AOP` `@PublishedOrder`로 표준화~~ ← 적절하지 않음
    - `AOP`는 횡단 관심사(`cross-cutting concern`)를 모듈화 하는데 적합하며, 특정 기능을 타겟하는 `AOP`는 오히려 비즈니스 로직이 가려지고, 가독성이 떨어지게 됨.
  - ~~`@TransactionalEventListener(AFTER_COMMIT)` 구간에서 `ZINCRBY` 호출~~
    - 먼저 단순하게 Redis 자료구조를 활용해 보는 것으로 구현
- 중복 집계 방지
  - 멱등 키(`RANK:PROCESSED:{orderId}`)를 함께 발급함

### Key & 자료구조

- `Redis` 자료구조
  - Sorted Set (ZSet): 상품 별 판매량을 score로 관리하여 정렬 및 집계에 용이함.
  - Key: `RANKING:PRODUCT:20250817`  
  - Value: member - productId, score - quantity(판매수량) 


## 테스트 코드 작성


## 기능 구현
