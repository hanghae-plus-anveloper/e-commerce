package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.OrderRepository;
import kr.hhplus.be.server.order.domain.OrderStatus;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.user.domain.User;
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
        User user = User.builder().name("홍길동").build();

        Product product1 = Product.builder().price(10000).build();
        Product product2 = Product.builder().price(5000).build();

        List<OrderItem> items = List.of(
                OrderItem.of(product1, 10000, 2, 0),   // 20,000
                OrderItem.of(product2, 5000, 1, 0)  // 5,000
        );

        Order expectedOrder = Order.create(user, items, 25000);
        when(orderRepository.save(any(Order.class))).thenReturn(expectedOrder);

        Order result = orderService.createOrder(user, items, 25000);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalAmount()).isEqualTo(25000);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.DRAFT);
    }

    @Test
    @DisplayName("유효한 쿠폰이 있으면, 총액에서 할인 금액이 적용된다")
    void createOrder_withValidCoupon_discountApplied() {
        User user = User.builder().name("홍길동").build();

        Product product1 = Product.builder().price(10000).build();
        Product product2 = Product.builder().price(5000).build();

        List<OrderItem> items = List.of(
                OrderItem.of(product1, 10000, 2, 0),   // 20,000
                OrderItem.of(product2, 5000, 1, 1000)  // 4,000
        ); // 총 24,000

        CouponPolicy mockPolicy = mock(CouponPolicy.class);
        when(mockPolicy.isWithinPeriod()).thenReturn(true);
        when(mockPolicy.getRemainingCount()).thenReturn(1);

        Coupon coupon = Coupon.builder()
                .policy(mockPolicy)
                .user(user)
                .discountAmount(4000)
                .discountRate(0)
                .used(false)
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(user, items, 20000);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(result.getTotalAmount()).isEqualTo(20000); // 24,000 - 4,000 = 20,000
    }

}
