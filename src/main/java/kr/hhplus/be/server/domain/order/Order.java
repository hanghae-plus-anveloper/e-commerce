package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public static Order create(Long userId, List<OrderItem> items, int totalAmount) {
        Order order = new Order();
        order.userId = userId;
        order.items.addAll(items);
        order.totalAmount = totalAmount;
        order.status = "PAID";
        order.createdAt = new Date();
        items.forEach(item -> item.setOrder(order));
        return order;
    }
}
