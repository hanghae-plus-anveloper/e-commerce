package kr.hhplus.be.server.coupon.application;


import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponWorker {

    private final CouponService couponService;
    private final CouponRedisService couponRedisService;

    @Scheduled(fixedDelay = 10_000) // 10초마다 실행
    public void syncActivePolicies() {
        List<CouponPolicy> activePolicies = couponService.getActivePolicies();

        // 현재 DB에 유효한 정책 ID
        List<Long> activePolicyIds = activePolicies.stream()
                .map(CouponPolicy::getId)
                .toList();

        // redis에 존재하는 정책 ID
        List<Long> redisPolicyIds = couponRedisService.getAllPolicyIds();

        // Redis에만 존재하는 정책 제거
        for (Long redisPolicyId : redisPolicyIds) {
            if (!activePolicyIds.contains(redisPolicyId)) {
                couponRedisService.removePolicy(redisPolicyId);
            }
        }

        // 유효한 정책은 Redis에 반영  + 남은 수량 포함
        for (CouponPolicy policy : activePolicies) {
            couponRedisService.setRemainingCount(policy.getId(), policy.getRemainingCount());
        }
    }

    @Scheduled(fixedDelay = 1000) // 1초마다 실행
    public void processAllPending() {
        List<Long> policyIds = couponRedisService.getAllPolicyIds();

        for (Long policyId : policyIds) {
            while (true) {
                List<Long> userIds = couponRedisService.peekPending(policyId, 500);

                if (userIds.isEmpty()) break;

                List<Long> succeeded = new ArrayList<>();

                for (Long userId : userIds) {
                    try {
                        couponService.issueCoupon(userId, policyId);
                        succeeded.add(userId);
                    } catch (Exception ignored) {
                    }
                }

                couponRedisService.removePending(policyId, succeeded);
            }
        }
    }
}
