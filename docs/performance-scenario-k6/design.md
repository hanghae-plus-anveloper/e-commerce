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
- ~~잔액이 예측했던 금액과 일치하는지 확인~~
- ~~쿠폰 적용된 100건 + 쿠폰 없는 200건 = 총 300건 주문 성공~~
  - **병목 체크를 위해 모두 쿠폰을 발급 해주는 상태 조건으로 변경** 
- 300명 동시 실행 시 DB, Redis, 분산락 처리 과정에서 병목이 없는지 확인
- SLA(95% 응답 200ms 이내) 충족 여부 검증

## K6 환경 세팅

### Grafana & InfluxDB 세팅

- 방법 1: philhawthorne/docker-influxdb-grafana 이미지 사용 (미사용)
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

- 방법 2: docker-compose-k6.yml 직접 작성, latest 버전 사용 ✅
  -  [docker-compose-k6.yml](https://github.com/hanghae-plus-anveloper/e-commerce/blob/develop/docker-compose-k6.yml)
    
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

- **결론**
  -  ~~K6 측정 항목들이 수년간 크게 변화가 없기때문에 템플릿 이미지를 사용해도 문제가 없을 것으로 판단됨~~ 
  -  ~~추후 최신 Grafana-InfluxDB-K6 호환 버전의 템플릿으로 Grafana 대시 보드 변경 예정~~
  - `docker-compose-k6.yml`로 최신 버전의 `influxDB` 와 `Grafana` 설치 사용
  - `K6` 최신 대시보드 인 **`22201`** 템플릿으로 변경

### 스크립트 실행

```bash
# 각 시나리오 별 ENV 에 따른 실행 방식 추가(하단에 개별 스크립트 추가)
docker-compose -f docker-compose-k6.yml run --rm k6 \
  run --out influxdb=http://k6:k6pass@influxdb:8086/k6 \
  /scripts/test.js
```

- `InfluxDB` + `Grafana` 이미지 기반으로 구성
- `k6` → `InfluxDB` → `Grafana` 데이터 파이프라인 확인
- `Grafana` 대시보드는 기본 k6 ~~2587~~ `22201` 템플릿 사용, ~~InfluxQL 기반 쿼리~~ API 정상 동작 여부 검증

## 테스트 코드

- [scripts/test.js](https://github.com/hanghae-plus-anveloper/e-commerce/blob/develop/scripts/test.js): 페르소나 A형(충동 구매형) 사용자에 대한 테스트 작성

### 테스트 시나리오 옵션 및 측정 값 세팅

<details><summary>세팅 코드</summary>

```js
let scenarios = {};

if (__ENV.SCENARIO === "flow_iterations") {
  scenarios = {
    flow_iterations: {
      executor: "per-vu-iterations",
      vus: 300,
      iterations: 1,
      maxDuration: "1m",
    },
  };
} else if (__ENV.SCENARIO === "flow_10vus") {
  scenarios = {
    flow_10vus: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 10 },
        { duration: "1m", target: 10 },
        { duration: "10s", target: 0 },
      ],
      gracefulStop: "10s",
    },
  };
} else if (__ENV.SCENARIO === "flow_100vus") {
  scenarios = {
    flow_100vus: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 100 },
        { duration: "1m", target: 100 },
        { duration: "10s", target: 0 },
      ],
      gracefulStop: "10s",
    },
  };
} else if (__ENV.SCENARIO === "flow_300vus") {
  scenarios = {
    flow_300vus: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 300 },
        { duration: "2m", target: 300 },
        { duration: "20s", target: 0 },
      ],
      gracefulStop: "20s",
    },
  };
}

export const options = { scenarios };

const BASE_URL = "http://host.docker.internal:8080";

// 미사용
const httpFailures = new Rate("http_req_failed");
const httpSuccess = new Rate("http_req_success");
const requests = new Counter("http_reqs");

// 성능 지표 // 미사용, 기본 tags 기능으로 조회가능
const chargeTime = new Trend("charge_balance_time");
const couponTime = new Trend("coupon_issue_time");
const couponCheckTime = new Trend("coupon_check_time");
const top5Time = new Trend("get_top5_time");
const orderTime = new Trend("order_time");
```
</details>

- 측정 지표 세팅(미사용)
  - ~~`httpFailures`, `httpSuccess`, `requests`: 기본 지표 중 실패율과 성공율 별도 관리~~
  - `chargeTime`: 잔액 충전 성능 지표
  - `couponTime`: 쿠폰 발급 시도 성능 지표
  - `couponCheckTime`: 비동기로 발급된 내 쿠폰 목록 조회 성능 지표
  - `top5Time`: 상위 상품 조회 성능 지표
  - `orderTime`: 주문 생성 성능 지표

- `22201` 대시보드 기본 수집 정보에 의해 `tags`를 `API`에 지정하는 경우 별도의 `Trend` 없이도 기본적으로 측정됨
- `__ENV.SCENARIO` 실행 스크립트에 따라 시나리오 구분 실행
  - `flow_iterations`: 1회 실행, 300VUs
  - `flow_10vus`: 소규모 사용자(10명) 상황에서 지표 확인
  - `flow_100vus`: 중간 부하(100명) 상황에서 지표 확인
  - `flow_300vus`: 최종 목적 부하(300명) 상황에서 지표 확인


### 측정치 기록

```js
// 미사용
const trackMetrics = (res, trendMetric) => {
  if (trendMetric) {
    trendMetric.add(res.timings.duration);
  }
  httpFailures.add(res.status >= 400);
  httpSuccess.add(res.status < 400);
  requests.add(1);
};
```
- 측정 기록 헬퍼 함수(미사용)
  - k6 기본 측정 지표, Tags로 대체

### 기본 데이터 초기화

```js
// DB 초기화
export const setup = () => {
  const url = `${BASE_URL}/init`;
  const res = http.post(url, null, {
    headers: { "Content-Type": "application/json" },
    tags: { name: "0_setup_init_data" },
  });
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

- [BootDataInitializer.java](https://github.com/hanghae-plus-anveloper/e-commerce/blob/main/src/main/java/kr/hhplus/be/server/boot/BootDataInitializer.java)

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
  check(res, {
    "getUserId 200": (r) => {
      if (r.status !== 200) {
        console.error(`Unexpected user status: ${r.status}, body=${r.body}`);
      }
      return r.status === 200;
    },
  });
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
// 5. 주문 생성
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
  check(res, {
    "order 201": (r) => {
      if (r.status !== 201) {
        console.error(`Unexpected order status: ${r.status}, body=${r.body}`);
      }
      return r.status === 201;
    },
  });
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
  
### 시나리오 별 실행 코드

- iterations 시나리오
```bash
docker-compose -f docker-compose-k6.yml run --rm \
  -e SCENARIO=flow_iterations \
  k6 run --out influxdb=http://k6:k6pass@influxdb:8086/k6 /scripts/test.js
```

- 10 VUs 시나리오
```bash
docker-compose -f docker-compose-k6.yml run --rm \
  -e SCENARIO=flow_10vus \
  k6 run --out influxdb=http://k6:k6pass@influxdb:8086/k6 /scripts/test.js
```

- 100 VUs 시나리오
```bash
docker-compose -f docker-compose-k6.yml run --rm \
  -e SCENARIO=flow_100vus \
  k6 run --out influxdb=http://k6:k6pass@influxdb:8086/k6 /scripts/test.js
```

- 300 VUs 시나리오
```bash
docker-compose -f docker-compose-k6.yml run --rm \
  -e SCENARIO=flow_300vus \
  k6 run --out influxdb=http://k6:k6pass@influxdb:8086/k6 /scripts/test.js
```

### 테스트 결과

- [Flow Iterations Grafana Link](https://snapshots.raintank.io/dashboard/snapshot/k4h45FVqWUgDUOsHXHMdPW02SzbHOnkv): 1회 실행, 300VUs
    ![Flow Iterations Success](./assets/001-flow-iterations-success.png)
  - 초기 데이터 세팅 및 API 정상 동작 검증을 목적으로 함
  - 모든 API 결과 정상 응답
    ![Flow Iterations 01](./assets/002-flow-iterations-grafana1.png)
    ![Flow Iterations 02](./assets/003-flow-iterations-grafana2.png)
  - **결과 요약**
    - 총 1,801건 요청 중 실패 0건 (성공률 100%)
    - 평균 응답 시간 `278ms`, 최대 `2.13s`
    - `order_time` 평균이 `1,027ms`로 가장 길게 소요됨
    - 동시 300명에서 초기 DB/Redis/쿠폰 정책 로딩은 정상 처리됨
    - 다만 주문 생성(`createOrder`)에서 평균 응답 시간이 높음 → 주문 로직이 성능 병목 지점으로 확인됨


- [Flow Ramping 10VUs Grafana Link](https://snapshots.raintank.io/dashboard/snapshot/x84XUCAgGJ7efa7Yq7How8Os5e12RDi4): 소규모 사용자(10명) 상황에서 지표 확인
    ![Flow Ramping 10VUs Success](./assets/021-flow-ramping-10vus-success.png)
  - 잔액 충전, 쿠폰 발급, 주문 생성 모두 정상 동작하고 있음
    ![Flow Ramping 10VUs 01](./assets/022-flow-ramping-10vus-grafana1.png)
    ![Flow Ramping 10VUs 02](./assets/023-flow-ramping-10vus-grafana2.png)
  - **결과 요약**
      - 총 4,489건 요청, 실패 0건
      - 평균 응답 시간 `10ms`, `order_time` 평균 `39.9ms`
      - 모든 지표 SLA(200ms 이내)를 충분히 만족
      - 소규모 사용자 환경에서는 안정적 성능 제공
      - 캐시 및 락 경합 부담이 미미한 수준


- [Flow Ramping 100VUs Grafana Link](https://snapshots.raintank.io/dashboard/snapshot/FifD66GJjpCQChabGPfYY1ClIj5EdKWS): 중간 부하(100명) 상황에서 지표 확인 
    ![Flow Ramping 100VUs Success](./assets/031-flow-ramping-100vus-success.png)
  - 잔액 충전, 쿠폰 발급, 주문 생성 모두 정상 동작하고 있음
    ![Flow Ramping 100VUs 01](./assets/032-flow-ramping-100vus-grafana1.png)
    ![Flow Ramping 100VUs 02](./assets/033-flow-ramping-100vus-grafana2.png)
  - **결과 요약**
      - 총 45,307건 요청, 실패 0건
      - 평균 응답 시간 `10.6ms`, `order_time` 평균 `50.9ms`
      - `P95` 기준 응답 시간도 대부분 50~200ms 구간
      - 100명 수준까지는 안정적으로 동작하며, `DB/Redis` 병목도 관찰되지 않음
      - `SLA` 기준 충족 (95% 응답 <200ms)


- [Flow Ramping 300VUs Grafana Link](https://snapshots.raintank.io/dashboard/snapshot/0n4hL64VL8yPGtg6ycokSWB60ufll9kS): 최종 목적 부하(300명) 상황에서 지표 확인
    ![Flow Ramping 300VUs Fail](./assets/041-flow-ramping-300vus-fail.png)
  - 3 스테이지 마지막 단계에서 `getUserId` 실패  - `99%(15579 성공 / 15 실패)`
  - 2 스테이지 중간 부터 `createOrder`함수에서 주문 생성 실패 - `96%(15098 성공 / 478 실패)`
  - 서버 에러로 `Multilock already unlocked` 에러 발생하여 주문 생성에 실패함
  - 주문 요청의 과부하로 Redis 기반의 락 경합 부분에서 정상 동작하지 않을 것으로 예측됨
  - 각 API 요청 중 가장 병목현상이 큰 `createOrder`에 대한 로직 개선 필요
    ![Flow Ramping 300VUs 01](./assets/042-flow-ramping-300vus-grafana1.png)
    ![Flow Ramping 300VUs 02](./assets/043-flow-ramping-300vus-grafana2.png)
  - **결과 요약**
    - 총 94,136건 요청 중 535건 실패 (실패율 0.56%)
    - 실패 구간: `getUserId`(13건), `getTopProducts`(6건), `createOrder`(516건)
    - `order_time` 평균 `1,174ms`, 최대 `3,100ms`
    - P95 기준 `~2.3s`, SLA 미충족
    - 300명 동시 실행 시부터 실패가 발생
    - 주요 실패 원인은 `createOrder`의 Redis 락 경합 및 서버 과부하
    - DB/Redis 리소스는 응답은 유지하나, 락 처리 과정에서 지연 및 unlock 오류 발생

## 개선 및 병목 해결 방안

위 시나리오별로 사용자를 증가시켜 테스트 한 결과 100VUs 까지는 오류 없이 동작하다가, 300VUs 설정 시 실패하는 상황이 발생함. 단순 코드 상의 문제만 인지, 실행 환경의 리소스 부족에 의한 실패 인지 확인이 필요함.

### 개선 고려 사항

- 서버 스펙 및 인프라 레벨의 확장
  - CPU 메모리 부족: 서버 인스턴스의 CPU 메모리가 급격히 증가되었을 가능성
  - DB 커넥션 수: 어플리케이션에서 통과하더라도, DB단에서 서버 한계에 도달했을 가능성
  - Redis 과부하: 요청이 몰려 Redis 기반의 락이 과부하로 정상 동작하지 못했을 가능성
  - 서버 스펙을 증설, DB/Redis의 자원을 확장하는 방향이 가장 공수가 적고(비용이 있는) 개선 방법으로 판단됨

- 서버 에러 로그 구체화
  - 도메인 레벨 뿐만 아니라, 시스템 상의 에러로그를 단순 500처리 하지 않고, 구체적으로 제공
  - **에러 로그의 원인인 Multi Lock이 사용되는 코드 개선**
  
- 재시도 로직 추가
  - 서버에서의 실패가 외부로 노출되지 않도록 재시도 로직 추가
  - 락 경합의 경우 재시도 로직을 추가해 주문 자체는 성공하도록 변경하여 사용자 경험 개선

## 병목 개선 

멀티락 에러가 출력되는 createOrder 관련하여 서버 환경 리소스를 확장하고, 멀티락 로직을 개선

### DB 설정 변경 - 해결방법 X

- [docker-compose.yml](https://github.com/hanghae-plus-anveloper/e-commerce/blob/develop/docker-compose.yml): MySQL Docker 에 세부 설정 추가
  ```yml
  version: '3.8'

  services:
    mysql:
      image: mysql:8.0
      container_name: hhplus-mysql
      ports:
        - "3306:3306"
      environment:
        MYSQL_ROOT_PASSWORD: root
        MYSQL_USER: application
        MYSQL_PASSWORD: application
        MYSQL_DATABASE: hhplus
      command: # 설정 추가
        --max_connections=1000 # 동시 접속자 수 
        --innodb_buffer_pool_size=2G # InnoDB 버퍼 풀 크기
        --innodb_log_file_size=512M # Redo Log 파일 크기
        --innodb_flush_log_at_trx_commit=2 # 트랜잭션 로그 플러시 정책
        --innodb_flush_method=O_DIRECT # 파일 IO 최적화
        --innodb_thread_concurrency=0 # 동시 쓰레드 제한, 0: CPU 기반
        --innodb_read_io_threads=8 # 읽기 IO 스레드 수
        --innodb_write_io_threads=8 # 쓰기 IO 스레드 수
        --performance_schema=OFF # Performance Schema 비활성화
        --skip-name-resolve # DNS 역조회 비활성화
      volumes:
        - ./data/mysql:/var/lib/mysql
      networks:
        - hhplus-network
      restart: unless-stopped
      deploy:
        resources:
          limits:
            cpus: "2.0"
            memory: 4g
          reservations:
            cpus: "1.0"
            memory: 2g

  volumes:
    mysql_data:

  networks:
    hhplus-network:
      driver: bridge

  ```
  
- [application.yml](https://github.com/hanghae-plus-anveloper/e-commerce/blob/main/src/main/resources/application.yml): Hikari 설정 변경

  ```yml
  spring:
    application:
      name: hhplus
    profiles:
      active: local

    datasource:
      name: HangHaePlusDataSource
      type: com.zaxxer.hikari.HikariDataSource
      driver-class-name: com.mysql.cj.jdbc.Driver
      hikari:
        maximum-pool-size: 50 # 리소스 증가
        minimum-idle: 10
        connection-timeout: 30_000 # 30초 (DB 커넥션을 가져오지 못하면 예외 발생)
        max-lifetime: 1_800_000 # 30분 (커넥션 최대 생존 시간)
        keepalive-time: 300_000
        initialization-fail-timeout: 0

  # /* ... */
  ```

### 추가 테스트 결과 - DB 성능 증가 효과 없음

![Flow Ramping 300vus Fail 2](./assets/051-flow-ramping-300vus-fail.png)
- 설정 변경 효과 없음
  ![Flow Ramping 300vus Fail Grafana 2 01](./assets/052-flow-ramping-300vus-grafana1.png)
  ![Flow Ramping 300vus Fail Grafana 2 02](./assets/053-flow-ramping-300vus-grafana2.png)
  - DB 성능 확장보다 근본적인 문제 접근이 필요함

### 에러 원인 직접 수정 - 해결 방법 O

#### 기존 멀티락 로직

- 주문 생성이 있는 OrderFacade의 주요 로직에서 `DistributedLock`을 사용중에 있음
  ```java
  @Component
  @RequiredArgsConstructor
  public class OrderFacade {
      /* ... */
      @Transactional
      @DistributedLock(prefix = LockKey.PRODUCT, ids = "#orderItems.![productId]")
      public Order placeOrder(Long userId, List<OrderItemCommand> orderItems, Long couponId) {
        /* ... */
      }
  }
  ```
  - 초기 구현 시에는 `productId` 뿐만 아니라, `couponId`나 `userId`까지 멀티락으로 사용했으나, 
  - 낙관적락과 조건부 업데이트 적용방식으로 변경하면서 `productId`만 멀티락으로 남게 됨

#### 수정된 멀티락 로직

- [OrderFacade.java](https://github.com/hanghae-plus-anveloper/e-commerce/blob/main/src/main/java/kr/hhplus/be/server/order/facade/OrderFacade.java): `DistributedLock` 제거
  ```java
  @Component
  @RequiredArgsConstructor
  public class OrderFacade {
    @Transactional
    // @DistributedLock(prefix = LockKey.PRODUCT, ids = "#orderItems.![productId]") // 제거
    public Order placeOrder(Long userId, List<OrderItemCommand> orderItems, Long couponId) {
      User user = userService.findById(userId);

      List<ProductReservationRequest> requests = orderItems.stream()
          .map(i -> new ProductReservationRequest(i.getProductId(), i.getQuantity()))
          .toList();

      // reserveProducts 함수 내에서 락 진행
      List<OrderItem> items = productService.reserveProducts(requests).stream()
          .map(r -> OrderItem.of(r.getProduct(), r.getPrice(), r.getQuantity(), 0))
          .toList();
        
      /* ... */
    }
  }
  ```
- [ProductService.java](https://github.com/hanghae-plus-anveloper/e-commerce/blob/main/src/main/java/kr/hhplus/be/server/product/application/ProductService.java): items를 받아서 재고를 차감하는 동안에만 `DistributedLock` 적용
  ```java
  @Service
  @RequiredArgsConstructor
  public class ProductService {
    /* ... */

    @Transactional
    @DistributedLock(prefix = LockKey.PRODUCT, ids = "#requests.![productId]")
    public List<ProductReservation> reserveProducts(List<ProductReservationRequest> requests) {
      return requests.stream()
          .sorted(Comparator.comparing(ProductReservationRequest::productId))
          .map(request -> {
              Product product = verifyAndDecreaseStock(request.productId(), request.quantity());
              return new ProductReservation(product, product.getPrice(), request.quantity());
          })
          .toList();
    }
  }
  ```
  - 트랜젝션 내에서 락이 동작하는 부분을 `Product`를 사용하는 곳으로 범위를 축소
  - 나머지 `OrderFacade`의 `User`, `Coupon`, `Balance`와 분리

### 추가 테스트 결과 - 멀티락 로직 개선 성공 ✅

- [Flow Ramping 300VUs Grafana Link - Order 실패 개선](https://snapshots.raintank.io/dashboard/snapshot/WrPCHT1sLipXqaS5HBUJebDzxvNahF7v)
  ![Flow Ramping 300vus Fail 3](./assets/061-flow-ramping-300vus-semi-success.png)
  - `createOrder` 실패 없어짐
  - `getUserId` 99%(18180 성공 / 10 실패)
  - `claimCoupon` 99%(18178 성공 / 2 실패)
    ![Flow Ramping 300vus Fail 3 Grafana 01](./assets/062-flow-ramping-300vus-grafana1.png)
    ![Flow Ramping 300vus Fail 3 Grafana 02](./assets/063-flow-ramping-300vus-grafana2.png)
    - 주문 요청에 대한 오류는 완전히 사라짐

## 결론

- 처음 300VUs에서 주문의 실패하는 요인은 로직상에 멀티락을 불필요하게 크게 잡는 것이 원인이었으며, 상품에 대한 멀티락을 상품을 차감하는 로직에 국한하여 개선하였음.
- 성능 측정을 300VUs까지 올려서 테스트 하지 않았다면 식별하지 못했을 수도 있는 코드상의 오류를 테스트를 통해 확인하고, 이를 개선함.
- 추후 `gracefulStop` 단계에서 발생하는 `getUserId`, `claimCoupon` 에러도 동일한 방식으로 로그를 확인하고 개선 예정.
- 이벤트 기반의 Saga 패턴으로 코레오그래피를 적용하여 OrderFacade에 밀집도를 없애고, 도메인 별로 책임을 분리하는 것을 목표로 함.

