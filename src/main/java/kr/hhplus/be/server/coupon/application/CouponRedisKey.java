package kr.hhplus.be.server.coupon.application;

public class CouponRedisKey {
    public static String remainingKey(Long policyId) {
        return "COUPON:POLICY:" + policyId + ":REMAINING";
    }
    public static String pendingKey(Long policyId) {
        return "COUPON:POLICY:" + policyId + ":PENDING";
    }
    public static String issuedKey(Long policyId) {
        return "COUPON:POLICY:" + policyId + ":ISSUED";
    }
}