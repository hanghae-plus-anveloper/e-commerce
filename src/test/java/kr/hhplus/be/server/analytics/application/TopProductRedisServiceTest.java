package kr.hhplus.be.server.analytics.application;


import kr.hhplus.be.server.IntegrationTestContainersConfig;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import kr.hhplus.be.server.order.facade.OrderFacade;
import kr.hhplus.be.server.order.facade.OrderItemCommand;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(IntegrationTestContainersConfig.class)
public class TopProductRedisServiceTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TopProductRedisService topProductRedisService;

    private User user;
    private Product p1, p2, p3, p4, p5, p6;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        user = userRepository.save(User.builder().name("ranking-user").build());
        balanceRepository.save(Balance.builder().user(user).balance(1_000_000).build());

        p1 = productRepository.save(Product.builder().name("p1").price(100).stock(100).build());
        p2 = productRepository.save(Product.builder().name("p2").price(100).stock(100).build());
        p3 = productRepository.save(Product.builder().name("p3").price(100).stock(100).build());
        p4 = productRepository.save(Product.builder().name("p4").price(100).stock(100).build());
        p5 = productRepository.save(Product.builder().name("p5").price(100).stock(100).build());
        p6 = productRepository.save(Product.builder().name("p6").price(100).stock(100).build());

        // 오늘 이전 데이터 추가(날짜를 받아서 redis에 직접 세팅)
        IntStream.rangeClosed(1, 5).forEach(i -> record(p2, 5, 1)); // 1일 전, 5회 * 5개
        IntStream.rangeClosed(1, 5).forEach(i -> record(p3, 4, 2)); // 2일 전, 5회 * 4개
        IntStream.rangeClosed(1, 5).forEach(i -> record(p4, 3, 1)); // 1일 전, 5회 * 3개
        IntStream.rangeClosed(1, 5).forEach(i -> record(p5, 2, 2)); // 2일 전, 5회 * 2개
        IntStream.rangeClosed(1, 5).forEach(i -> record(p6, 1, 3)); // 3일 전, 5회 * 1개 - 조회되지 않아야 함
    }


    @Test
    @DisplayName("최근 3일 간 누적 집계를 통해 Top5 상품을 Redis 집계 정보를 기반으로 조회한다")
    void top5InLast3DaysWithRedis() {

        List<TopProductRankingDto> before = topProductRedisService.getTop5InLast3Days();
        System.out.println("오늘자 주문 발생 전 집계: " + before);

        // 오늘 자 주문 생성, 생성 시 Redis에 추가 되는 지, 집계 결과에 합산 되는 지 확인
        IntStream.rangeClosed(1, 5).forEach(i -> place(p1, 10)); // 오늘, 5회 * 10개 = 50개 TOP 1
        IntStream.rangeClosed(1, 4).forEach(i -> place(p2, 1)); // 오늘, 4회 * 1개
        IntStream.rangeClosed(1, 3).forEach(i -> place(p3, 1)); // 오늘, 3회 * 1개
        IntStream.rangeClosed(1, 2).forEach(i -> place(p4, 1)); // 오늘, 2회 * 1개
        IntStream.rangeClosed(1, 1).forEach(i -> place(p5, 1)); // 오늘, 2회 * 1개

        List<TopProductRankingDto> after = topProductRedisService.getTop5InLast3Days();
        System.out.println("오늘자 주문 발생 후 집계: " + after);

        assertThat(after).hasSize(5);
        assertThat(after.get(0).productId()).isEqualTo(p1.getId().toString());
    }

    private void place(Product product, int quantity) {
        orderFacade.placeOrder(user.getId(), List.of(new OrderItemCommand(product.getId(), quantity)), null);
    }

    // 오늘 날짜 이전(어제 ~ ) 데이터는 service에 직접 주입(집계용)
    private void record(Product product, int qty, int daysAgo) {
        topProductRedisService.recordOrder(product.getId().toString(), qty);
    }
}
