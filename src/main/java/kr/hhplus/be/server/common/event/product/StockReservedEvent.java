package kr.hhplus.be.server.common.event.product;

public record StockReservedEvent(
        Long orderId,
        Long subTotalAmount
) {
}
