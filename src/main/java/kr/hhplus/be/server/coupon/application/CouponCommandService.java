package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.common.event.coupon.CouponUseFailedEvent;
import kr.hhplus.be.server.common.event.coupon.CouponUsedEvent;
import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.common.lock.LockKey;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import kr.hhplus.be.server.coupon.exception.InvalidCouponException;
import kr.hhplus.be.server.saga.domain.OrderSagaItem;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponCommandService {

    private final CouponRepository couponRepository;
    private final ApplicationEventPublisher publisher;

    @Transactional
    @DistributedLock(prefix = LockKey.COUPON, ids = "#couponId")
    public void useCoupon(Long couponId, Long orderId, List<OrderSagaItem> items) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new InvalidCouponException("존재하지 않는 쿠폰입니다. id=" + couponId));

        try {
            coupon.use();
            couponRepository.save(coupon);

            int discountAmount = coupon.getDiscountAmount();
            double discountRate = coupon.getDiscountRate();

            publisher.publishEvent(new CouponUsedEvent(orderId, couponId, discountAmount, discountRate));
        } catch (Exception e) {
            publisher.publishEvent(new CouponUseFailedEvent(orderId, couponId, e.getMessage(), items));
            throw e;
        }
    }

    @Transactional
    @DistributedLock(prefix = LockKey.COUPON, ids = "#couponId")
    public void skipCoupon(Long orderId) {
        publisher.publishEvent(new CouponUsedEvent(orderId, null, 0,0.0));
    }

    @Transactional
    @DistributedLock(prefix = LockKey.COUPON, ids = "#couponId")
    public void restoreCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new InvalidCouponException("존재하지 않는 쿠폰입니다. id=" + couponId));
        coupon.restore();
        couponRepository.save(coupon);
    }
}
