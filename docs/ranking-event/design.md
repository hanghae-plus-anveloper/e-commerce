# E-commerce Order Additional Event

## 부가 로직에 대한 관심사 분리 
### 목적

> 주문 생성 시 핵심 트랜잭션과 부가기능(집계, 알림 등)을 분리하여 안정성과 가독성 확보

### 설계 방향

- 주문 생성은 `OrderFacade` 내에서 트랜잭션 단위로 처리
- 주문 생성 완료 시점에 **주문 완료 이벤트(`OrderCompletedEvent`)** 발행
- 부가 로직은 별도 이벤트 핸들러에서 수행

### 이벤트 흐름

- `OrderFacade.placeOrder()`
  -  사용자 검증, 상품 재고 차감, 쿠폰 사용, 잔액 차감, 주문 생성 처리
  - 완료 후 `OrderCompletedEvent` 발행
- `TopProductEventHandler`
  - `OrderCompletedEvent` 수신
  - 주문 상품 정보를 기반으로 랭킹 집계 수행

### 기능 구현 전 테스트 코드

- 기존에 구현한 테스트 코드가 성공하는 상태가 유지 되도록 이벤트 방식의 기능으로 수정되어야 함
- 기능 구현 전 테스트 코드 성공 상태 확인
![성공이 유지되는 코드](./assets/001-top-product-success.png)
  - `setUp`: 2~6번 상품 Redis에 직접 세팅
    - 6번 상품은 3일전으로 집계되지 않음
  - 오늘자 주문 생성 후 1번 상품이 1위로 집계되고,
  - 2~5번 상품 또한 오늘 발생한 주문 수량만큼 추가 집계됨

## 기능 구현

### 이벤트 관련 클래스 구현

- [OrderCompletedEvent.java](https://github.com/hanghae-plus-anveloper/hhplus-e-commerce-java/blob/develop/src/main/java/kr/hhplus/be/server/common/event/OrderCompletedEvent.java)
  ```java
  package kr.hhplus.be.server.common.event;
  
  import kr.hhplus.be.server.analytics.application.TopProductRankingDto;
  
  import java.util.List;
  
  public record OrderCompletedEvent(
          Long orderId,
          Long userId,
          List<TopProductRankingDto> rankingDtoList
  ) {
  }
  ```
  - 주문 생성 성공 이벤트

- [TopProductEventHandler.java](https://github.com/hanghae-plus-anveloper/hhplus-e-commerce-java/blob/develop/src/main/java/kr/hhplus/be/server/analytics/application/TopProductEventHandler.java)
  ```java
  @Component
  @RequiredArgsConstructor
  public class TopProductEventHandler {
  
    private final TopProductService topProductService;
  
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(OrderCompletedEvent event) {
      topProductService.recordOrdersAsync(event.orderId(), event.rankingDtoList());
    }
  } 
  ```
  - `AFTER_COMMIT` 에 의해 트랜젝션이 커밋 되고 나서 이벤트 동작으로 구현
  - 주문 생성 함수의 `OrderCompletedEvent`를 수집하여 `TopProductService`에 집계 요청

- [TopProductService.java](https://github.com/hanghae-plus-anveloper/hhplus-e-commerce-java/blob/develop/src/main/java/kr/hhplus/be/server/analytics/application/TopProductService.java)
  ```java
  @Service
  @RequiredArgsConstructor
  public class TopProductService {
      /* ... */
      @Async
      public void recordOrdersAsync(Long orderId, List<TopProductRankingDto> items) {
          if (redisRepository.isAlreadyIssued(orderId)) {
              return;
          }
          try {
              redisRepository.recordOrders(items.stream().map(TopProductMapper::toRecord).toList());
              redisRepository.markIssued(orderId);
          } catch (Exception ignored) {
          }
      }
      /* ... */
  }
  ```
  - 기록된 주문인지 확인
  - 상품 번호 및 수량 배열로 RedisRepository에 기록
  - 주문 번호 기록

- [TopProductRedisRepository.java](https://github.com/hanghae-plus-anveloper/hhplus-e-commerce-java/blob/develop/src/main/java/kr/hhplus/be/server/analytics/infrastructure/TopProductRedisRepository.java)
  ```java
  @Repository
  @RequiredArgsConstructor
  public class TopProductRedisRepository {
      /* ... */
    
      private static final int TTL_DAYS = 4;
      private static final String PRODUCT_RANKING_PREFIX = "RANKING:PRODUCT:";
      private static final String ISSUED_ORDER_SET = PRODUCT_RANKING_PREFIX + "ISSUED";
  
      private String getDailyKey(LocalDate date) {
          return PRODUCT_RANKING_PREFIX + date.format(FORMATTER);
      }
  
      public boolean isAlreadyIssued(Long orderId) {
          return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ISSUED_ORDER_SET, orderId.toString()));
      }
  
      public void markIssued(Long orderId) {
          redisTemplate.opsForSet().add(ISSUED_ORDER_SET, orderId.toString());
          redisTemplate.expire(ISSUED_ORDER_SET, Duration.ofDays(TTL_DAYS));
      }
  
      public void recordOrders(List<TopProductRecord> items) {
          String key = getDailyKey(LocalDate.now());
  
          for (TopProductRecord item : items) {
              redisTemplate.opsForZSet().incrementScore(key, item.productId(), item.soldQty());
          }
  
          redisTemplate.expire(key, Duration.ofDays(TTL_DAYS));
      }
      
      /* ... */
  }
  ```
  - `isAlreadyIssued`: SET 자료구조의 중복 발급 확인 함수
  - `markIssued`: 상품별 수량 증가 후 완료된 주문 기록, TTL은 집계함수와 동일하게 설정
  - `recordOrders`: 상품 배열 기반으로 `날짜별` 판매량 을 ZSET에 기록

### 테스트 성공 상태 확인

![성공 유지중인 테스트 코드](./assets/002-top-product-success.png)
- 주문 완료 후 이벤트 기반으로 `TopProductEventHandler`가 정보를 수집하여 Redis 집계가 정상적으로 이루어짐을 확인
- 기존 테스트(최근 3일간 Top5 조회)는 이벤트 방식 적용 이후에도 그대로 성공 상태 유지
- 핵심 트랜잭션과 분리된 부가 로직이 이벤트 → 핸들러 구조로 대체되었음에도 기능 동작과 정합성은 변하지 않음