package kr.hhplus.be.server.coupon.facade;


import kr.hhplus.be.server.coupon.application.CouponRedisService;
import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponFacade {

    private final CouponService couponService;
    private final CouponRedisService couponRedisService;

    @Transactional
    public Boolean tryIssue(Long userId, Long policyId) {
        return couponRedisService.tryIssue(userId, policyId);
    }

    @Transactional
    public Coupon issueCoupon(Long userId, Long policyId) {
        return couponService.issueCoupon(userId, policyId);
    }

    @Transactional
    public Coupon useCoupon(Long userId, Long policyId) {
        return couponService.useCoupon(policyId, userId);
    }

    @Transactional(readOnly = true)
    public List<Coupon> getCoupons(Long userId) {
        return couponService.getCoupons(userId);
    }
}
