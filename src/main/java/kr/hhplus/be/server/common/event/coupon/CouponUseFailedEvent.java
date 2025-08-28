package kr.hhplus.be.server.common.event.coupon;

public record CouponUseFailedEvent(
        Long orderId,
        String reason
) {
}
