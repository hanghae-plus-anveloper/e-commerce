package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "ORDER_ITEM")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private int price;
    private int quantity;
    private int discountAmount;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    public static OrderItem of(Long productId, int price, int quantity, int discountAmount) {
        OrderItem item = new OrderItem();
        item.productId = productId;
        item.price = price;
        item.quantity = quantity;
        item.discountAmount = discountAmount;
        return item;
    }

    public int getSubtotal() {
        return (price * quantity) - discountAmount;
    }
}
