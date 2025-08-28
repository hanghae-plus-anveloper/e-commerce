package kr.hhplus.be.server.common.event.order;

public record OrderFailedEvent(
        Long orderId,
        String reason
) {
}
