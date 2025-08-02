package kr.hhplus.be.server.product.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class) // 생략 가능
class ProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        product = productRepository.save(Product.builder()
                .name("상품X")
                .price(10000)
                .stock(50)
                .build());
    }

    @Test
    @DisplayName("상품을 저장하고 조회할 수 있다 - 통합 테스트")
    void saveAndRetrieve() {
        Product found = productRepository.findById(product.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("상품X");
        assertThat(found.getStock()).isEqualTo(50);
    }
}
