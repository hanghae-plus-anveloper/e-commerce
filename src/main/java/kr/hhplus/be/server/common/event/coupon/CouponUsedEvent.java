package kr.hhplus.be.server.common.event.coupon;

public record CouponUsedEvent(
        Long orderId,
        Long discountAmount,
        Long couponId
) {
}
