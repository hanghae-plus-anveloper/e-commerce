package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponPolicy;
import kr.hhplus.be.server.domain.coupon.CouponPolicyRepository;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.exception.InvalidCouponException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public Coupon issueCoupon(User user, Long policyId) {
        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(() -> new InvalidCouponException("존재하지 않는 쿠폰 정책입니다."));

        if (!policy.isWithinPeriod()) {
            throw new InvalidCouponException("쿠폰 정책이 유효하지 않습니다.");
        }

        policy.decreaseRemainingCount();

        Coupon coupon = Coupon.builder()
                .policy(policy)
                .userId(user.getId())
                .discountAmount(policy.getDiscountAmount())
                .discountRate(policy.getDiscountRate())
                .used(false)
                .build();

        return couponRepository.save(coupon);
    }

    @Transactional(readOnly = true)
    public List<Coupon> getCoupons(Long userId) {
        return couponRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Coupon findValidCouponOrThrow(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new InvalidCouponException("쿠폰을 찾을 수 없습니다."));

        if (!coupon.isAvailable()) {
            throw new InvalidCouponException("사용할 수 없는 쿠폰입니다.");
        }

        return coupon;
    }
}
