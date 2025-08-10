package kr.hhplus.be.server.coupon.facade;


import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponFacade {
    private final UserService userService;
    private final CouponService couponService;

    @Transactional
    public Coupon issueCoupon(Long userId, Long policyId) {
        User user = userService.findById(userId);
        return couponService.issueCoupon(user, policyId);
    }

    @Transactional(readOnly = true)
    public List<Coupon> getCoupons(Long userId) {
        return couponService.getCoupons(userId);
    }
}
