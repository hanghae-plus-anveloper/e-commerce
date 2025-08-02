package kr.hhplus.be.server.order.domain;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Order와 OrderItem을 저장 할 수 있다")
    void saveOrderWithItems() {
        User user = userRepository.save(User.builder().name("성진").build());
        Product product = productRepository.save(Product.builder()
                .name("상품A")
                .price(10000)
                .build());

        OrderItem item = OrderItem.of(product, 10000, 2, 1000);
        Order order = Order.create(user, List.of(item), item.getSubtotal());

        Order saved = orderRepository.save(order);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getProduct().getName()).isEqualTo("상품A");
    }
}
