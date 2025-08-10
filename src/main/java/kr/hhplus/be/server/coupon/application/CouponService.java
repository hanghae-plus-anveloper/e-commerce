package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import kr.hhplus.be.server.coupon.exception.CouponSoldOutException;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.coupon.exception.InvalidCouponException;
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

//        policy.decreaseRemainingCount(); // 도메인에서 감소

        int updated = couponPolicyRepository.decreaseRemainingCount(policyId);
        if (updated == 0) {
            throw new CouponSoldOutException("남은 쿠폰 수량이 없습니다.");
        }

        Coupon coupon = Coupon.builder()
                .policy(policy)
                .user(user)
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

    @Transactional
    public Coupon useCoupon(Long couponId, Long userId) {
        Coupon coupon = findValidCouponOrThrow(couponId, userId);
        // coupon.use();

        int updated = couponRepository.markCouponAsUsed(couponId, userId); // 조건부 update
        if (updated == 0) {
            throw new InvalidCouponException("이미 사용된 쿠폰이거나 유효하지 않습니다.");
        }
        return couponRepository.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new InvalidCouponException("쿠폰 조회 실패"));
    }
}
