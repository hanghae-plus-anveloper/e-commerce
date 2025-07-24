package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponPolicy;
import kr.hhplus.be.server.domain.coupon.CouponPolicyRepository;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.exception.CouponSoldOutException;
import kr.hhplus.be.server.exception.InvalidCouponException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
