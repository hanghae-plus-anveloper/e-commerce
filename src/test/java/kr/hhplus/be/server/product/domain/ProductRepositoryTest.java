package kr.hhplus.be.server.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품을 저장하고 ID로 조회할 수 있다")
    void saveAndFindById() {
        Product product = Product.builder()
                .name("상품A")
                .price(5000)
                .stock(10)
                .build();

        Product saved = productRepository.save(product);
        Product found = productRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("상품A");
        assertThat(found.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("재고를 차감할 수 있다")
    void decreaseStockSuccess() {
        Product product = productRepository.save(
                Product.builder()
                        .name("상품B")
                        .price(10000)
                        .stock(20)
                        .build()
        );

        product.decreaseStock(5);

        assertThat(product.getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("재고보다 많이 차감하려 하면 예외가 발생한다")
    void decreaseStockFail() {
        Product product = productRepository.save(
                Product.builder()
                        .name("상품C")
                        .price(10000)
                        .stock(3)
                        .build()
        );

        assertThatThrownBy(() -> product.decreaseStock(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("재고가 부족합니다.");
    }
}
