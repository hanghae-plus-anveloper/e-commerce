package kr.hhplus.be.server.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "`ORDER`")
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

    public static Order create(Long userId, List<OrderItem> items, int totalAmount) {
        Order order = new Order();
        order.userId = userId;
        order.items.addAll(items);
        order.status = OrderStatus.DRAFT;
        order.createdAt = new Date();
        items.forEach(item -> item.setOrder(order));
        order.totalAmount = totalAmount;
        return order;
    }
}
