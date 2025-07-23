package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.coupon.Coupon;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private int totalAmount;

    @Setter
    private OrderStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public static Order create(Long userId, List<OrderItem> items, Coupon coupon) {
        Order order = new Order();
        order.userId = userId;
        order.items.addAll(items);
        order.status = OrderStatus.DRAFT;
        order.createdAt = new Date();
        items.forEach(item -> item.setOrder(order));


        int total = items.stream()
                .mapToInt(OrderItem::getSubtotal)
                .sum();

//        if (coupon != null && coupon.isAvailable()) {
//            coupon.use();
//            int discount = coupon.getDiscountAmount();
//            total = Math.max(0, total - discount);
//        }

        order.totalAmount = total;
        return order;
    }
}
