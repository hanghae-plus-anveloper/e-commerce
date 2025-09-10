import { check, sleep } from "k6";
import http from "k6/http";
import { Counter, Rate, Trend } from "k6/metrics";

export const options = {
  scenarios: {
    ecommerce_flow: {
      executor: "per-vu-iterations",
      vus: 300,
      iterations: 1,
      maxDuration: "1m",
    },
    ecommerce_flow: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 50 },
        { duration: "30s", target: 150 },
        { duration: "30s", target: 300 },
        { duration: "1m", target: 300 },
        { duration: "30s", target: 0 },
      ],
      gracefulStop: "30s",
    },
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

// DB 초기화
export const setup = () => {
  const url = `${BASE_URL}/init`;
  const res = http.post(url, null, { headers: { "Content-Type": "application/json" } });
  check(res, { "init 200": (r) => r.status === 200 });
  const body = res.json();
  console.log("System initialized:", JSON.stringify(body));
  return { policyId: body.policyId };
};

// 0. 사용자 이름으로 사용자 ID 확인
const getUserIdByName = (vuName) => {
  const url = `${BASE_URL}/users/id?name=${vuName}`;
  const res = http.get(url, { tags: { name: "0_get_userId" } });
  trackMetrics(res);
  check(res, { "getUserId 200": (r) => r.status === 200 });
  return res.json();
};

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

// 2. 쿠폰 발급 시도
const claimCoupon = (userId, policyId) => {
  const url = `${BASE_URL}/coupons?userId=${userId}&policyId=${policyId}`;
  const res = http.post(url, null, { tags: { name: "2_claim_coupon" } });

  trackMetrics(res, couponTime);
  check(res, { "coupon response valid": (r) => [201, 400, 409].includes(r.status) });
  return res.status === 201;
};

// 3. 사용자 보유 쿠폰 확인 (약간 대기 후)
const getUserCoupons = (userId) => {
  sleep(1); // 비동기 발급 처리 대기
  const url = `${BASE_URL}/users/${userId}/coupons`;
  const res = http.get(url, { tags: { name: "3_get_coupons" } });

  trackMetrics(res, couponCheckTime);
  check(res, { "get coupons 200": (r) => r.status === 200 });
  return res.json() || [];
};

// 4. 인기 상품 조회
const getTopProducts = () => {
  const url = `${BASE_URL}/products/top/realtime`;
  const res = http.get(url, { tags: { name: "4_get_top_products" } });

  trackMetrics(res, top5Time);
  check(res, { "top5 200": (r) => r.status === 200 });
  return res.json() || [];
};

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

// 메인 시나리오
const test = (data) => {
  const { policyId } = data;
  const vuName = `u${__VU % 300 || 300}`;
  const userId = getUserIdByName(vuName);

  const balance = chargeBalance(userId);
  console.log(`User\t${userId}\tbalance:\t${balance.balance}\t`);

  const issued = claimCoupon(userId, policyId);
  const coupons = issued ? getUserCoupons(userId) : [];
  if (coupons.length > 0) {
    console.log(`User\t${userId}\tcoupon:\t${coupons[0].couponId}\t`);
  }

  const top5 = getTopProducts().slice(0, 5);
  if (top5.length > 0) {
    const order = createOrder(userId, top5, coupons?.[0]);
    console.log(`User\t${userId}\torder:\t${order.orderId}\tcoupon:\t${coupons?.[0]?.couponId || ""}`);
  }
};

const trackMetrics = (res, trendMetric) => {
  if (trendMetric) {
    trendMetric.add(res.timings.duration);
  }
  httpFailures.add(res.status >= 400);
  httpSuccess.add(res.status < 400);
  requests.add(1);
};

export default test;
