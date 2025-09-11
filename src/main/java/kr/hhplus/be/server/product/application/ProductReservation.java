package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductReservation {
    private final Product product;
    private final int price;
    private final int quantity;

    public int getSubtotal() {
        return price * quantity;
    }
}