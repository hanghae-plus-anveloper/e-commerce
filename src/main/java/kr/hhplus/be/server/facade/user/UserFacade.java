package kr.hhplus.be.server.facade.user;

import kr.hhplus.be.server.application.balance.BalanceService;
import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.application.user.UserService;
import kr.hhplus.be.server.controller.user.BalanceResponseDto;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final BalanceService balanceService;
    private final CouponService couponService;

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userService.findById(userId);
    }

    @Transactional
    public void chargeBalance(Long userId, int amount) {
        User user = userService.findById(userId);
        balanceService.chargeBalance(user, amount);
    }

    @Transactional
    public void useBalance(Long userId, int amount) {
        User user = userService.findById(userId);
        balanceService.useBalance(user, amount);
    }

    @Transactional(readOnly = true)
    public BalanceResponseDto getBalance(Long userId) {
        User user = userService.findById(userId);
        return new BalanceResponseDto(user.getId(), user.getBalance().getBalance());
    }

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