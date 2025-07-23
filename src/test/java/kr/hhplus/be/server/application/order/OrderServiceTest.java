package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderService = new OrderService(orderRepository);
    }

    @Test
    @DisplayName("(쿠폰 없이) 주문을 생성하면, 총액이 항목별 합계로 계산된다")
    void createOrder() {
        Long userId = 1L;
        List<OrderItem> items = List.of(
                OrderItem.of(1_000_001L, 10000, 2, 0),   // 20,000
                OrderItem.of(1_000_002L, 5000, 1, 1000)  // 4,000
        );

        Order expectedOrder = Order.create(userId, items, null);
        when(orderRepository.save(any(Order.class))).thenReturn(expectedOrder);

        Order result = orderService.createOrder(userId, items, null);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalAmount()).isEqualTo(24000);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.DRAFT);
    }
}
