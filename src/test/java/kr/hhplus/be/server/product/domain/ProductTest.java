package kr.hhplus.be.server.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("재고를 차감하면 남은 재고가 줄어든다")
    void decreaseStockSuccess() {
        Product product = Product.builder()
                .name("상품")
                .price(10000)
                .stock(10)
                .build();

        product.decreaseStock(3);

        assertThat(product.getStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("차감하려는 수량이 현재 재고보다 많으면 예외가 발생한다")
    void decreaseStockFail() {
        Product product = Product.builder()
                .name("상품")
                .price(10000)
                .stock(2)
                .build();

        assertThatThrownBy(() -> product.decreaseStock(5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("재고가 부족합니다.");
    }
}
