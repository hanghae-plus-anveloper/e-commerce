package kr.hhplus.be.server.order.facade;

import lombok.Getter;

@Getter
public class OrderItemCommand {
    private final Long productId;
    private final int quantity;

    public OrderItemCommand(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}
