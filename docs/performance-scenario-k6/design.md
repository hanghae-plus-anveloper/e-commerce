# E-commerce Performance Scenario K6

## 성능 테스트 시나리오

### 페르소나 지정

#### 페르소나 A 유형 - 충동 구매형 소비 패턴 ✅

- 배경
  - 쿠폰 이벤트를 보면 반드시 참여하려는 성향
  - 남들이 다 있는 상품은 무조건 구매하는 성향
  - 광고에 취약해, 충동적인 구매를 하는 사용자를 모티브로 페르소나 지정
- 행동 단계
  - 상품 1~5번을 모두 구매할 수 있는 충분한 금액을 충전함
  - 충전이 완료되면 발급받은 사용자 ID를 기반으로 할인 쿠폰 발행을 시도함
  - 쿠폰 발급 성공 여부에 관계 없이 주문을 시도함
  - 쿠폰 ID를 포함하여, 상품은 3일 간 가장 많이 판매된 TOP5를 각 1개씩 구매함
  - 주문에 성공하면, 최종적으로 남은 잔액을 확인함
- 테스트 포인트
  - 잔액 충전 성공 100% 
  - 쿠폰 발급 여부에 관계 없이 주문을 시도 
  - 상품 5개가 담긴 주문 생성

#### 페르소나 B 유형 - 합리적 소비 패턴

- 배경
  - 쿠폰 유무에 따른 합리적인 소비를 하려는 성향
  - 원래 구매하려던 상품 1개 외에 쿠폰 유무에 따라 추가 상품 1개를 더 구매
  - 할인, 1+1 이벤트 등, 소비를 하되 합리적인 소비를 목적으로 하는 사용자를 모티브로 페르소나 지정
- 행동 단계
  - 상품 1번을 구매할 수 있는 금액을 충전함
  - 충전이 완료되면 발급된 사용자 ID를 기반으로 할인 쿠폰 발행을 시도함
  - 쿠폰 발급 유무에 따라, 할인금액에 맞춰 상품 2번을 추가로 주문하거나, 원래 구매하려는 1번만 구매함
  - 최종적으로 남은 잔액을 확인하여 0원인 지 확인함
- 테스트 포인트 
  - 잔액 충전 성공 100%
  - 쿠폰 발급 여부에 따라 상품 변동
  - 최종적으로 잔액을 모두 소비

#### 페르소나 C 유형 - 악의적인 주문 시도 패턴

- 배경
  - 평소 서비스의 취약점을 노려 악의적인 주문을 시도함
  - 발급되지 않는 쿠폰을 사용하여 주문을 시도함
  - 쿠폰이 발급되었다면, 쿠폰을 동시에 두번 사용하려고 시도함
- 행동 단계
  - 충분하지 않은 금액 충전
  - 쿠폰 발급 시도
  - 발급되었다면 중복 사용 시도, 발급되지 않았어도 무작위 요청 시도
- 테스트 포인트
  - 전 구간에서 실패하여 주문이 발생하지 않아야 함

### 시나리오 개요

- 동시 최대 300명의 사용자가 **페르소나 A(충동 구매형)** 플로우를 수행하는 동안의 성능 및 안정성을 검증함을 목적으로 함.
- 핵심 플로우는 잔액 충전 → 한정 수량에 대한 쿠폰 발급 시도 → 여러 상품을 한번에 구매 순으로 진행함.
- 잔액 충전은 각자 정상적으로 충전이 되며, 쿠폰 발급은 300개의 요청 중 100건만 성공하고, 쿠폰 사용 여부와 관계없이 300건의 주문이 생성되어야 함.
  - 병목 체크 테스트를 위해 쿠폰이 모두 발급 되는 경우도 함께 확인 

### 전체 성능 테스트 체크 항목

- 300명의 가상 사용자 전부가 페르소나 A(충동 구매형) 시나리오로 동작
- 모두 충분한 금액을 충전하고, 쿠폰 발급 시도
- 정책 설정에 따라 100건만 쿠폰 발급 성공, 나머지 200건은 실패
- 쿠폰 발급 실패 여부와 상관없이 300건의 주문 모두 정상 생성
- 쿠폰 적용된 100건 + 쿠폰 없는 200건 = 총 300건 주문 성공
  - 병목 체크를 위해 코두 쿠폰을 발급 해주는 상태 조건으로 변경 
- 잔액이 예측했던 금액과 일치하는지 확인
- 300명 동시 실행 시 DB, Redis, 분산락 처리 과정에서 병목이 없는지 확인
- SLA(95% 응답 200ms 이내) 충족 여부 검증

## K6 환경 세팅

### Grafana & InfluxDB 세팅

- 방법 1: philhawthorne/docker-influxdb-grafana 이미지 사용
  - https://hub.docker.com/r/philhawthorne/docker-influxdb-grafana/ 
  - Grafana K6 대시보드 템플릿(2587)과 호환되는 Docker 이미지 사용
    ```bash
    docker run -d \
      --name docker-influxdb-grafana \
      -p 3000:3000 \
      -p 8083:8083 \
      -p 8086:8086 \
      -v ./grafana/influxdb:/var/lib/influxdb \
      -v ./grafana/grafana:/var/lib/grafana \
      philhawthorne/docker-influxdb-grafana:latest
    ```
  - 특이사항: 마지막 업데이트가 5년 전

- 방법 2: docker-compose-k6.yml 직접 작성, latest 버전 사용
  -  [docker-compose-k6.yml](https://github.com/hanghae-plus-anveloper/hhplus-e-commerce-java/blob/develop/docker-compose-k6.yml)
    
      <details><summary>세부 코드</summary>

      ```yml
      services:
        influxdb:
          image: influxdb:1.8
          container_name: influxdb
          ports:
            - "8086:8086"
          environment:
            - INFLUXDB_DB=k6
            - INFLUXDB_ADMIN_USER=admin
            - INFLUXDB_ADMIN_PASSWORD=admin123
            - INFLUXDB_USER=k6
            - INFLUXDB_USER_PASSWORD=k6pass
          volumes:
            - influxdb-data:/var/lib/influxdb

        grafana:
          image: grafana/grafana:latest
          container_name: grafana
          ports:
            - "3000:3000"
          environment:
            - GF_SECURITY_ADMIN_USER=admin
            - GF_SECURITY_ADMIN_PASSWORD=admin
            - GF_USERS_ALLOW_SIGN_UP=false
          volumes:
            - grafana-data:/var/lib/grafana
          depends_on:
            - influxdb

        k6:
          image: grafana/k6:latest
          container_name: k6
          depends_on:
            - influxdb
          volumes:
            - ./scripts:/scripts
          working_dir: /scripts

      volumes:
        influxdb-data:
        grafana-data:
      ```
      </details>  

  - `Grafana` 최신 버전에서 `InfluxQL`이 아닌 `Flux`로 쿼리가 변경되면서 `K6 2578` 템플릿 사용 시 대부분의 패널들이 No Data로 뜨는 상황이 발생하여 템플릿 사용불가
  - 직접 패널을 추가시에는 지정한 Trend나 vus 기본 측정값이 확인 가능

- 결론
  -  K6 측정 항목들이 수년간 크게 변화가 없기때문에 템플릿 이미지를 사용해도 문제가 없을 것으로 판단됨 
  -  추후 최신 Grafana-InfluxDB-K6 호환 버전의 템플릿으로 Grafana 대시 보드 변경 예정

### 스크립트 실행

```bash
docker-compose -f docker-compose-k6.yml run --rm k6 \
  run --out influxdb=http://k6:k6pass@influxdb:8086/k6 \
  /scripts/test.js
```

- InfluxDB + Grafana 이미지 기반으로 구성
- k6 → InfluxDB → Grafana 데이터 파이프라인 확인
- Grafana 대시보드는 기본 k6 2587 템플릿 사용, InfluxQL 기반 쿼리 정상 동작 여부 검증

## 테스트 코드

- [scripts/test.js](https://github.com/hanghae-plus-anveloper/hhplus-e-commerce-java/blob/develop/scripts/test.js): 페르소나 A형(충동 구매형) 사용자에 대한 테스트 작성

### 테스트 시나리오 옵션 및 측정 값 세팅

<details><summary>세팅 코드</summary>

```js

export const options = {
  scenarios: {
    // e_commerce_flow: {
    //   executor: "per-vu-iterations",
    //   vus: 300,
    //   iterations: 1,
    //   maxDuration: "1m",
    // },
    e_commerce_flow_ramping: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "1m", target: 100 },
        { duration: "1m", target: 200 },
        { duration: "1m", target: 300 },
        { duration: "2m", target: 300 },
        { duration: "30s", target: 0 },
      ],
      gracefulStop: "30s",
      startTime: "10s",
      // startTime: "1m",
    },
    // flow_10vus: {
    //   executor: "ramping-vus",
    //   startVUs: 0,
    //   stages: [
    //     { duration: "30s", target: 10 },
    //     { duration: "1m", target: 10 },
    //     { duration: "10s", target: 0 },
    //   ],
    //   gracefulStop: "10s",
    // },
    // flow_100vus: {
    //   executor: "ramping-vus",
    //   startVUs: 0,
    //   stages: [
    //     { duration: "30s", target: 100 },
    //     { duration: "1m", target: 100 },
    //     { duration: "10s", target: 0 },
    //   ],
    //   gracefulStop: "10s",
    //   startTime: "2m",
    // },
    // flow_300vus: {
    //   executor: "ramping-vus",
    //   startVUs: 0,
    //   stages: [
    //     { duration: "30s", target: 300 },
    //     { duration: "2m", target: 300 },
    //     { duration: "20s", target: 0 },
    //   ],
    //   gracefulStop: "20s",
    //   startTime: "4m",
    // },
  },
};

const BASE_URL = "http://host.docker.internal:8080";

const httpFailures = new Rate("http_req_failed");
const httpSuccess = new Rate("http_req_success");
const requests = new Counter("http_reqs");

// 성능 지표
const chargeTime = new Trend("charge_balance_time");
const couponTime = new Trend("coupon_issue_time");
const couponCheckTime = new Trend("coupon_check_time");
const top5Time = new Trend("get_top5_time");
const orderTime = new Trend("order_time");
```
</details>

- `httpFailures`, `httpSuccess`, `requests`: 기본 지표 중 실패율과 성공율 별도 관리
- `chargeTime`: 잔액 충전 성능 지표
- `couponTime`: 쿠폰 발급 시도 성능 지표
- `couponCheckTime`: 비동기로 발급된 내 쿠폰 목록 조회 성능 지표
- `top5Time`: 상위 상품 조회 성능 지표
- `orderTime`: 주문 생성 성능 지표

### 측정치 기록 

```js
const trackMetrics = (res, trendMetric) => {
  if (trendMetric) {
    trendMetric.add(res.timings.duration);
  }
  httpFailures.add(res.status >= 400);
  httpSuccess.add(res.status < 400);
  requests.add(1);
};
```
- 측정 기록 헬퍼 함수

### 기본 데이터 초기화

```js
// DB 초기화
export const setup = () => {
  const url = `${BASE_URL}/init`;
  const res = http.post(url, null, { headers: { "Content-Type": "application/json" } });
  check(res, { "init 200": (r) => r.status === 200 });
  const body = res.json();
  console.log("System initialized:", JSON.stringify(body));
  return { policyIds: body.policyIds };
};
```

- 사용자 생성 u1 ~ u300 
  - 사용자 회원가입 기능이 없기 때문에 DB에 300명의 사용자를 미리 생성
  - 추가로, vus 증가 시를 대응하기 위해, 사용자 이름으로 id를 조회 시 없는 사용자인 경우 생성해서 userId를 반환하도록 API 수정
- 쿠폰 정책은 테스트 실행 시 5개를 생성하고, 생서된 policyIds를 setup 함수에서 반환 받아서 테스트에 사용
- 상품 정보의 경우 1~5번 상품을 생성하고, 이에 대한 redis에 Top5 상위 주문을 미리 세팅

- [BootDataInitializer.java](https://github.com/hanghae-plus-anveloper/hhplus-e-commerce-java/blob/develop/src/main/java/kr/hhplus/be/server/boot/BootDataInitializer.java)

    <details><summary>DB 세팅 코드</summary>

    ```java
    @Slf4j
    @Component
    @Profile("local")
    @RequiredArgsConstructor
    public class BootDataInitializer implements ApplicationRunner {
      /* ... */

      private List<User> seedUsers() {
        List<User> batch = new ArrayList<>(USER_COUNT);
        for (int i = 1; i <= USER_COUNT; i++) {
          batch.add(User.builder().name("u" + i).build());
        }
        List<User> saved = userRepository.saveAll(batch);
        log.info("[INIT] seeded users: {}", saved.size());
        return saved;
      }

      // 기본 상품 세팅
      private List<Product> seedProducts() {
        List<Product> batch = new ArrayList<>(PRODUCT_COUNT);
        for (int i = 1; i <= PRODUCT_COUNT; i++) {
          Product p = Product.builder().name("p" + i).price(PRODUCT_PRICE).stock(PRODUCT_STOCK).build();
          batch.add(p);
        }
        List<Product> saved = productRepository.saveAll(batch);
        log.info("[INIT] seeded products: {}", saved.stream().map(Product::getId).toList());
        return saved;
      }

      private List<CouponPolicy> seedCouponPolicies() {
        LocalDateTime now = LocalDateTime.now();
        List<CouponPolicy> policies = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
          CouponPolicy policy = CouponPolicy.builder()
              .discountAmount(COUPON_DISCOUNT_AMOUNT)
              .discountRate(COUPON_DISCOUNT_RATE)
              .availableCount(COUPON_AVAILABLE)
              .remainingCount(COUPON_REMAINING)
              .expireDays(COUPON_EXPIRE_DAYS)
              .startedAt(now.minusMinutes(1))
              .endedAt(now.plusDays(3))
              .build();
          policy = couponPolicyRepository.save(policy);
          policies.add(policy);

          try {
            couponRedisRepository.removePolicy(policy.getId());
            couponRedisRepository.setRemainingCount(policy.getId(), policy.getRemainingCount());
            log.info("[INIT][Redis] COUPON:POLICY:{} reset (remaining={}) 2", 
                      policy.getId(), policy.getRemainingCount());
          } catch (Exception e) {
            log.warn("[INIT][Redis] coupon remaining reset failed: {}", e.getMessage(), e);
          }
        }

        log.info("[INIT] seeded {} coupon policies", policies.size());
        return policies;
      }

      // TOP 5 Redis 세팅
      private void seedTop5Last3Days(List<Product> products) {
        try {
          topProductService.clearAll();
        } catch (Exception e) {
          log.warn("[INIT][Redis] top-product clearAll failed: {}", e.getMessage(), e);
        }

        LocalDate today = LocalDate.now();
        int[] scores = {500, 400, 300, 200, 100};

        for (int d = 0; d < 3; d++) {
          LocalDate date = today.minusDays(d);
          for (int i = 0; i < products.size() && i < scores.length; i++) {
            Long pid = products.get(i).getId();
            topProductService.recordOrder(String.valueOf(pid), scores[i], date);
          }
        }
        log.info("[INIT][Redis] top-product last3days seeded");
      }
      /* ... */
    }
    ```
    </details>

  - 부팅 시, K6에 의해 API 요청 시 사용자, 쿠폰 정책, 상품에 대한 데이터 세팅
  - 테스트 코드에서 예측할 수 없는 `policyIds`만 API 초기화 결과로 반환하여 테스트에 활용

### 사용자 ID 조회

```js
// 0. 사용자 이름으로 사용자 ID 확인
const getUserIdByName = (vuName) => {
  const url = `${BASE_URL}/users/id?name=${vuName}`;
  const res = http.get(url, { tags: { name: "0_get_userId" } });
  trackMetrics(res);
  check(res, { "getUserId 200": (r) => r.status === 200 });
  return res.json();
};
```

- vus 수에 따라 사용자 이름 u1 ~ u300 기반으로 id 조회
- vus가 증가하여 넘어가는 경우에 대응하여 없는 사용자의 경우 userId를 생성해서 반환

### 잔액 충전

```js
// 1. 잔액 충전
const chargeBalance = (userId) => {
  const url = `${BASE_URL}/users/${userId}/balance`;
  const payload = JSON.stringify({ amount: 10000 });
  const res = http.post(url, payload, {
    headers: { "Content-Type": "application/json" },
    tags: { name: "1_charge_balance" },
  });
  trackMetrics(res, chargeTime);
  check(res, { "charge 201": (r) => r.status === 201 });
  return res.json();
};
```
- 사용자 ID 기반 잔액 충전 시도

### 쿠폰 발급 시도

```js
// 2. 쿠폰 발급 시도
const claimCoupon = (userId, policyId) => {
  const url = `${BASE_URL}/coupons?userId=${userId}&policyId=${policyId}`;
  const res = http.post(url, null, { tags: { name: "2_claim_coupon" } });

  trackMetrics(res, couponTime);
  // check(res, { "coupon response valid": (r) => [201, 400, 409].includes(r.status) });
  check(res, {
    "coupon response valid": (r) => {
      if (![201, 400, 409].includes(r.status)) {
        console.error(`Unexpected coupon status: ${r.status}, body=${r.body}`);
      }
      return [201, 400, 409].includes(r.status);
    },
  });
  return res.status === 201;
};
```
- 사용자 ID와 쿠폰 정책 ID 기반으로 발급 시도 
- 201: Redis에 의해 발급 큐에 적재
- 400, 409: 잘못된 요청, 수량 소진 값 반환 확인

### 사용자 보유 쿠폰 조회

```js
// 3. 사용자 보유 쿠폰 확인 (약간 대기 후)
const getUserCoupons = (userId) => {
  sleep(1); // 비동기 발급 처리 대기
  const url = `${BASE_URL}/users/${userId}/coupons`;
  const res = http.get(url, { tags: { name: "3_get_coupons" } });

  trackMetrics(res, couponCheckTime);
  check(res, { "get coupons 200": (r) => r.status === 200 });
  return res.json() || [];
};
```

- 비동기로 발급 확정된 사용자 보유 쿠폰 조회

### 상위 상품 조회

```js
// 4. 인기 상품 조회
const getTopProducts = () => {
  const url = `${BASE_URL}/products/top/realtime`;
  const res = http.get(url, { tags: { name: "4_get_top_products" } });

  trackMetrics(res, top5Time);
  check(res, { "top5 200": (r) => r.status === 200 });
  return res.json() || [];
};
```

- 세팅된 정보 + 추가로 요청된 주문 정보 기반 상위 상품 조회
- 사용자는 상위 상품 5개를 모두 1개씩 구매

### 주문 생성

```js
// 5. 주문 생성 (쿠폰 있으면 적용)
const createOrder = (userId, products, coupon) => {
  const url = `${BASE_URL}/orders`;
  const payload = JSON.stringify({
    userId,
    couponId: coupon ? coupon.couponId : null,
    items: products.map((p) => ({ productId: p.productId, quantity: 1 })),
  });

  const res = http.post(url, payload, {
    headers: { "Content-Type": "application/json" },
    tags: { name: "5_create_order" },
  });

  trackMetrics(res, orderTime);
  check(res, { "order 201": (r) => r.status === 201 });
  return res.json();
};
```

- 사용자 ID, 상위 상품 목록, 발급된 쿠폰 ID 기반 주문 생성 측정
- 상품의 개당 가격은 2,000원이며, 할인이 없는 경우에는 10,000원, 쿠폰이 사용되면 8,000원이 소비됨

### 테스트 실행 

```js
// 메인 시나리오
const test = (data) => {
  const { policyIds } = data;
  const vuName = `u${__VU}`;
  const userId = getUserIdByName(vuName);

  const balance = chargeBalance(userId);
  // console.log(`User\t${userId}\tbalance:\t${balance.balance}\t`);

  const policyId = policyIds[__VU % policyIds.length];
  const issued = claimCoupon(userId, policyId);
  const coupons = issued ? getUserCoupons(userId) : [];
  if (coupons.length > 0) {
    // console.log(`User\t${userId}\tcoupon:\t${coupons[0].couponId}\t`);
  }

  const top5 = getTopProducts().slice(0, 5);
  if (top5.length > 0) {
    const order = createOrder(userId, top5, coupons?.[0]);
    // console.log(`User\t${userId}\torder:\t${order.orderId}\tcoupon:\t${coupons?.[0]?.couponId || ""}\t`);
  }
};

export default test;
```
- 사용자 조회 → 잔액 충전 → 쿠폰 발급 시도 → 쿠폰 조회 → 상위 상품 조회 → 주문 생성
- 구현한 대부분의 기능을 테스트 할 수 있는 전체 시나리오 작성
- k6 http 함수에 의해 동기적으로 결과값을 반환받아 다음 API에 활용하도록 구현
- 쿠폰 정책 ID의 경우 별도의 이름이 없어 setup으로 DB 초기화 시에 policyIds를 반환받아 테스트에서 활용
- 사용자 ID의 경우 1~300은 예측이 가능하여 setup에서 300명의 사용자를 미리 만들지만, vus 증가 시를 대응하기 위해 getUserIdByName 호출 시 없는 계정의 경우 생성하여 ID 반환됨
  

### 테스트 결과

-
-


## 개선 및 병목 해결 방안

- **DB Connection Pool**

  - 부하 테스트 시 커넥션 풀 부족 여부 모니터링
  - 필요한 경우 max pool size 상향 및 idle timeout 조정

- **Redis 락 처리**

  - 상품 재고, 쿠폰 발급에서 분산락 사용 → lock 경합으로 인한 지연 발생 가능성 점검
  - 멀티락 조기 해제 문제 발생 시 락 범위 및 TTL 조정 고려

- **애플리케이션 레벨 최적화**

  - Coupon 발급 로직: 불필요한 DB round trip 제거, Redis TTL 조정
  - Order 생성 로직: 비동기 처리 가능한 이벤트는 @TransactionalEventListener(AFTER\_COMMIT) 활용

- **지표 기반 대응**

  - `order_time` Trend에서 평균, P95 응답 시간 분석
  - SLA 미충족 구간 발견 시 API 레벨 분리, CQRS 또는 캐시 도입 고려

- **확장성 고려**

  - 단일 정책에 집중된 쿠폰 발급 요청 → 여러 정책으로 분산하여 테스트 가능
  - 상품 개수 확장 및 분산 조회 → Top N 상품 조회 성능 개선


