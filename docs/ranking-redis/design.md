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


## 통합 테스트

### 통합 테스트 목적

- 각각의 주문이 생성되면, Redis에 집계되어야 한다.
- 오늘을 포함한 3일 치의 상품에 대한 ID와 판매수량을 확인할 수 있어야 한다.

### 조건

- [TopProductRedisServiceTest.java](https://github.com/hanghae-plus-anveloper/hhplus-e-commerce-java/blob/develop/src/test/java/kr/hhplus/be/server/analytics/application/TopProductRedisServiceTest.java)
- 사용자는 1명으로 고정하여 순수하게 상품과 판매 수량만 집계되는 정보로 통제
- 잔액이나 상품의 수량은 충분하여 주문이 실패하지 않음
- 과거 1~3일 전의 데이터는 `setUp` 단계에서 미리 세팅(Product 1번 제외)
  - 실제로 시스템의 날짜를 바꿔가면서 넣어보려 하였으나, 
  - 주요 로직에 집중하고자 이전 데이터는 미리 넣는 것으로 구현했습니다.
  ```java
  @BeforeEach
  void setUp() {     
      /* ... */
  
      // 오늘 이전 데이터 추가(날짜를 받아서 redis에 직접 세팅)
      IntStream.rangeClosed(1, 5).forEach(i -> record(p2, 5, 1)); // 1일 전, 5회 * 5개
      IntStream.rangeClosed(1, 5).forEach(i -> record(p3, 4, 2)); // 2일 전, 5회 * 4개
      IntStream.rangeClosed(1, 5).forEach(i -> record(p4, 3, 1)); // 1일 전, 5회 * 3개
      IntStream.rangeClosed(1, 5).forEach(i -> record(p5, 2, 2)); // 2일 전, 5회 * 2개
      IntStream.rangeClosed(1, 5).forEach(i -> record(p6, 1, 3)); // 3일 전, 5회 * 1개 - 조회되지 않아야 함
  }
  ```

- 오늘자의 주문은 실제 Redis에 정보를 발행하는 OrderFacade를 통해 직접 호출
  - 오늘자 주문 생성 전 어제자까지의 집계 상태 확인
  - 오늘자 주문 생성 후 상품 5개가 반환되는지 확인
  - 오늘 가장 주문을 많이 생성한 1번 상품이 3일 전체에서 1번인지 확인
  ```java
    @Test
    @DisplayName("최근 3일 간 누적 집계를 통해 Top5 상품을 Redis 집계 정보를 기반으로 조회한다")
    void top5InLast3DaysWithRedis() {

        List<TopProductRankingDto> before = topProductRedisService.getTop5InLast3Days();
        System.out.println("오늘자 주문 발생 전 집계: " + before);

        // 오늘 자 주문 생성, 생성 시 Redis에 추가 되는 지, 집계 결과에 합산 되는 지 확인
        IntStream.rangeClosed(1, 5).forEach(i -> place(p1, 10)); // 오늘, 5회 * 10개 = 50개 TOP 1
        IntStream.rangeClosed(1, 4).forEach(i -> place(p2, 1)); // 오늘, 4회 * 1개
        IntStream.rangeClosed(1, 3).forEach(i -> place(p3, 1)); // 오늘, 3회 * 1개
        IntStream.rangeClosed(1, 2).forEach(i -> place(p4, 1)); // 오늘, 2회 * 1개
        IntStream.rangeClosed(1, 1).forEach(i -> place(p5, 1)); // 오늘, 2회 * 1개

        List<TopProductRankingDto> after = topProductRedisService.getTop5InLast3Days();
        System.out.println("오늘자 주문 발생 후 집계: " + after);

        assertThat(after).hasSize(5);
        assertThat(after.get(0).productId()).isEqualTo(p1.getId().toString());
    }
  ```
 
### 실패 테스트 작성 결과
 
![실패 - 컴파일만 성공 상태](./assets/001-ranking-place-order-fail.png)

- 컴파일만 되는 상태 구현
  - 빈 배열만 반환



## 기능 구현
