package kr.hhplus.be.server.common.event.coupon;

public record CouponUsedEvent(
        Long orderId,
        Integer discountAmount,
        Long couponId
) {
}
