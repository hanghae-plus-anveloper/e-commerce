package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProductServiceConcurrencyTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        product = productRepository.save(Product.builder()
                .name("상품-A")
                .price(10000)
                .stock(10)  // 초기 재고 10개
                .build());
    }

    @Test
    @DisplayName("동시에 여러 사용자가 재고를 차감하면 초과 차감되지 않아야 한다")
    void decreaseStock_concurrently_not_exceed() throws InterruptedException {
        int threadCount = 20; // 요청은 20번
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<CompletableFuture<Void>> futures = IntStream.rangeClosed(1, threadCount).mapToObj(i -> CompletableFuture.runAsync(() -> {
            try {
                productService.verifyAndDecreaseStock(product.getId(), 1);
            } catch (Exception e) {
                // 예외 발생 무시: 재고 부족
            } finally {
                latch.countDown();
            }
        }, executor)).toList();

        latch.await();

        Product result = productRepository.findById(product.getId())
                .orElseThrow();

        System.out.println("최종 재고: " + result.getStock());
        List<Product> all = productRepository.findAll();
        assertThat(result.getStock()).isGreaterThanOrEqualTo(0);
        assertThat(result.getStock()).isEqualTo(0); // 10개보다 적게 차감되면 안됨
    }
}
