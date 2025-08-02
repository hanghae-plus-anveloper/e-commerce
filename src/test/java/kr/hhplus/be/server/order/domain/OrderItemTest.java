package kr.hhplus.be.server.order.domain;

import kr.hhplus.be.server.product.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {

    @Test
    @DisplayName("OrderItem은 주문 시점의 금액으로 소계를 계산한다")
    void subtotalCalculation() {
        Product product = Product.builder().name("세제").price(3000).build();

        OrderItem item = OrderItem.of(product, 3000, 3, 500);
        int subtotal = item.getSubtotal(); // getSubtotal 함수 테스트

        assertThat(subtotal).isEqualTo(8500);
    }

    @Test
    @DisplayName("OrderItem에 orderedAt과 연관관계 필드를 설정할 수 있다")
    void setFields() {
        OrderItem item = OrderItem.of(null, 1000, 1, 0);

        LocalDateTime now = LocalDateTime.now();
        item.setOrderedAt(now);
        Product product = Product.builder().name("상품").price(1000).build();
        item.setProduct(product);
        Order order = new Order(); // Order 생성 시 시간이 OrderItem 에도 부여되는 지 테스트

        item.setOrder(order);

        assertThat(item.getOrderedAt()).isEqualTo(now);
        assertThat(item.getProduct()).isEqualTo(product);
        assertThat(item.getOrder()).isEqualTo(order);
    }
}
