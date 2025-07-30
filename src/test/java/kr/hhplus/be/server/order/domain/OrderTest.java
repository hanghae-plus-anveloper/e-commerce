package kr.hhplus.be.server.order.domain;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    @DisplayName("Order를 생성할 수 있다")
    void createOrder_success() {
        User user = User.builder().name("주문자").build();
        Product product = Product.builder().name("상품A").price(10000).stock(5).build();
        OrderItem item = OrderItem.of(product, 10000, 2, 2000);

        Order order = Order.create(user, List.of(item), item.getSubtotal());

        assertThat(order.getUser()).isEqualTo(user);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getTotalAmount()).isEqualTo(18000); // (10000 * 2 - 2000)
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getOrder()).isEqualTo(order);
        assertThat(order.getItems().get(0).getOrderedAt()).isEqualTo(order.getOrderedAt());
    }
}
