package kr.hhplus.be.server.order.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.user.domain.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "`order`", indexes = {
        @Index(name = "idx_order_user_status", columnList = "user_id, status"), // 유저별 주문 상태 조회, 사용자 관점 비즈니스 목적
        // @Index(name = "idx_order_ordered_at", columnList = "ordered_at"), // 날짜 필터, 3일 이내 TOP 5 조회 목적, 방법 1
})
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

    private LocalDateTime orderedAt;

    @Setter
    @Column(name = "ordered_date")
    private LocalDate orderedDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public static Order create(User user, List<OrderItem> items, int totalAmount) {
        Order order = new Order();
        order.user = user;
        order.status = OrderStatus.DRAFT;
        order.orderedAt = LocalDateTime.now();
        order.orderedDate = LocalDate.now();

        for (OrderItem item : items) {
            item.setOrder(order);
            item.setOrderedAt(order.orderedAt); // 반정규화된 필드 동기화
            item.setOrderedDate(order.orderedDate); // 반정규화된 필드 동기화
        }

        order.items.addAll(items);
        order.totalAmount = totalAmount;
        return order;
    }
}
