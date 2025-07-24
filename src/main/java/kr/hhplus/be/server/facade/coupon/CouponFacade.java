package kr.hhplus.be.server.facade.coupon;


import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.application.user.UserService;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.user.User;
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
