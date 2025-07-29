package kr.hhplus.be.server.order.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.user.domain.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "`order`")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private int totalAmount;

    @Setter
    private OrderStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date orderedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public static Order create(User user, List<OrderItem> items, int totalAmount) {
        Order order = new Order();
        order.user = user;
        order.items.addAll(items);
        order.status = OrderStatus.DRAFT;
        order.orderedAt = new Date();
        for (OrderItem item : items) {
            item.setOrder(order);
            item.setOrderedAt(order.orderedAt); // 반정규화된 필드 동기화
        }

        order.items.addAll(items);
        order.totalAmount = totalAmount;
        return order;
    }
}
