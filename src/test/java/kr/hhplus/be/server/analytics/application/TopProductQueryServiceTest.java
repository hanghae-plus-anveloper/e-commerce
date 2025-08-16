package kr.hhplus.be.server.analytics.application;

import kr.hhplus.be.server.IntegrationTestContainersConfig;
import kr.hhplus.be.server.analytics.domain.TopProductView;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.OrderItemRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationTestContainersConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TopProductQueryServiceTest {

    @Autowired
    private TopProductQueryService queryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        productRepository.deleteAll();

        // 더미 상품 생성
        Product p1 = productRepository.save(Product.builder().name("p1").price(100).stock(10_000).build());
        Product p2 = productRepository.save(Product.builder().name("p2").price(100).stock(10_000).build());
        Product p3 = productRepository.save(Product.builder().name("p3").price(100).stock(10_000).build());
        Product p4 = productRepository.save(Product.builder().name("p4").price(100).stock(10_000).build());
        Product p5 = productRepository.save(Product.builder().name("p5").price(100).stock(10_000).build());
        Product p6 = productRepository.save(Product.builder().name("p6").price(100).stock(10_000).build());

        // 3일치 총 주문 아이템 생성
        seedOrderItems(p1, 50);
        seedOrderItems(p2, 40);
        seedOrderItems(p3, 30);
        seedOrderItems(p4, 20);
        seedOrderItems(p5, 10);
        seedOrderItems(p6, 5);
    }

    @DisplayName("최근 3일(어제 ~ 그저께) Top5 상품을 Native Repository를 통해 조회한다")
    @Test
    void top5InLast3Days() {
        queryService.top5InLast3Days();

        // 쿼리 수행 20회 기록
        StopWatch sw = new StopWatch();
        sw.start("top5-query-x20");
        List<TopProductView> result = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            result = queryService.top5InLast3Days();
        }
        sw.stop();
        System.out.println(sw.prettyPrint());

        // 순위 검증
        assertThat(result).hasSize(5);
        assertThat(result.get(0).soldQty()).isGreaterThanOrEqualTo(result.get(1).soldQty());
        assertThat(result.get(1).soldQty()).isGreaterThanOrEqualTo(result.get(2).soldQty());
        assertThat(result.get(2).soldQty()).isGreaterThanOrEqualTo(result.get(3).soldQty());
        assertThat(result.get(3).soldQty()).isGreaterThanOrEqualTo(result.get(4).soldQty());

        List<Long> ids = result.stream().map(TopProductView::productId).toList();
        assertThat(ids).containsExactlyInAnyOrderElementsOf(
                productRepository.findAll().stream()
                        .filter(p -> List.of("p1", "p2", "p3", "p4", "p5").contains(p.getName()))
                        .map(Product::getId)
                        .toList()
        );
    }

    // 총 판매량을 3일에 나눠서 주문 아이템 생성
    private void seedOrderItems(Product product, int totalQty) {
        LocalDateTime now = LocalDateTime.now();

        int day1 = totalQty / 3;
        int day2 = totalQty / 3;
        int day3 = totalQty - day1 - day2;

        persistItems(product, day1, now.minusDays(1).toLocalDate(), now.minusDays(1)); // 어제
        persistItems(product, day2, now.minusDays(2).toLocalDate(), now.minusDays(2)); // 그제
        persistItems(product, day3, now.minusDays(3).toLocalDate(), now.minusDays(3)); // 그저께
    }

    // 주문 아이템 생성
    private void persistItems(Product product, int count, LocalDate orderedDate, LocalDateTime orderedAt) {
        if (count <= 0) return;

        for (int i = 0; i < count; i++) {
            OrderItem oi = OrderItem.of(product, product.getPrice(), 1, 0);
            oi.setOrderedDate(orderedDate);
            oi.setOrderedAt(orderedAt);
            orderItemRepository.save(oi);
        }
    }
}
