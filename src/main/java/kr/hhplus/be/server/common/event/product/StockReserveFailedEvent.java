package kr.hhplus.be.server.common.event.product;

public record StockReserveFailedEvent(
        Long orderId,
        String reason
) {
}
