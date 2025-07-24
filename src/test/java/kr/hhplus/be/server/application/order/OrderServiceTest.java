package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponPolicy;
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

    @Test
    @DisplayName("유효한 쿠폰이 있으면, 총액에서 할인 금액이 적용된다")
    void createOrder_withValidCoupon_discountApplied() {
        Long userId = 1L;
        List<OrderItem> items = List.of(
                OrderItem.of(1_000_001L, 10000, 2, 0),   // 20,000
                OrderItem.of(1_000_002L, 5000, 1, 1000)  // 4,000
        ); // 총 24,000

        CouponPolicy mockPolicy = mock(CouponPolicy.class);
        when(mockPolicy.isWithinPeriod()).thenReturn(true);
        when(mockPolicy.getRemainingCount()).thenReturn(1);

        Coupon coupon = Coupon.builder()
                .policy(mockPolicy)
                .userId(userId)
                .discountAmount(4000)
                .discountRate(0)
                .used(false)
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(userId, items, coupon);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(result.getTotalAmount()).isEqualTo(20000); // 24,000 - 4,000 = 20,000
    }

}
