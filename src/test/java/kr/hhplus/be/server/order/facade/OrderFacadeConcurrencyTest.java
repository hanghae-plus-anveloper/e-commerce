package kr.hhplus.be.server.order.facade;

import kr.hhplus.be.server.IntegrationTestContainersConfig;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import kr.hhplus.be.server.order.domain.Order;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(IntegrationTestContainersConfig.class)
public class OrderFacadeConcurrencyTest {


    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    // 사용자 별 발급된 쿠폰
    private final Map<Long, Long> couponIdByUser = new HashMap<>();

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();
        couponPolicyRepository.deleteAll();
        productRepository.deleteAll();
        balanceRepository.deleteAll();
        userRepository.deleteAll();

        //
        for (int i = 1; i <= 5; i++) {
            User u = userRepository.save(User.builder().name("user-" + i).build());
            balanceRepository.save(Balance.builder()
                    .user(u)
                    .balance(1_000_000)
                    .build());
        }

        // 더미 상품 생성, 재고 10
        for (int i = 1; i <= 5; i++) {
            productRepository.save(Product.builder()
                    .name("product-" + i)
                    .price(100)
                    .stock(10)
                    .build());
        }

        // 유효한 쿠폰 정책 > 쿠폰에 의한 실패 배제
        CouponPolicy policy = couponPolicyRepository.save(
                CouponPolicy.builder()
                        .discountRate(0.1)
                        .discountAmount(0)
                        .availableCount(1000)
                        .remainingCount(1000)
                        .startedAt(LocalDateTime.now().minusDays(1))
                        .endedAt(LocalDateTime.now().plusDays(1))
                        .expireDays(30)
                        .build()
        );

        // 사용자별 쿠폰 사용 추가
        userRepository.findAll().forEach(u -> {
            Coupon c = couponService.issueCoupon(u, policy.getId());
            couponIdByUser.put(u.getId(), c.getId());
        });
    }

    @Test
    @DisplayName("")
    void placeOrders_concurrently() throws InterruptedException {
        List<User> users = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
        List<Product> products = productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getId))
                .toList();

        IntFunction<Long> userId = i -> users.get(i - 1).getId();
        IntFunction<Long> productId = i -> products.get(i - 1).getId();

        Map<Long, List<OrderItemCommand>> carts = new LinkedHashMap<>();

        carts.put(userId.apply(1), List.of(
                new OrderItemCommand(productId.apply(3), 2),
                new OrderItemCommand(productId.apply(1), 3),
                new OrderItemCommand(productId.apply(5), 2)
        ));
        carts.put(userId.apply(2), List.of(
                new OrderItemCommand(productId.apply(1), 3),
                new OrderItemCommand(productId.apply(3), 2),
                new OrderItemCommand(productId.apply(5), 2)
        ));
        carts.put(userId.apply(3), List.of(
                new OrderItemCommand(productId.apply(2), 2),
                new OrderItemCommand(productId.apply(4), 2),
                new OrderItemCommand(productId.apply(1), 3)
        ));
        carts.put(userId.apply(4), List.of(
                new OrderItemCommand(productId.apply(4), 3),
                new OrderItemCommand(productId.apply(2), 2),
                new OrderItemCommand(productId.apply(1), 3)
        ));
        carts.put(userId.apply(5), List.of(
                new OrderItemCommand(productId.apply(5), 3),
                new OrderItemCommand(productId.apply(3), 2),
                new OrderItemCommand(productId.apply(2), 2)
        ));

        int threads = carts.size();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        AtomicInteger success = new AtomicInteger();
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        carts.forEach((uid, commands) -> executor.submit(() -> {
            try {
                start.await();
                Long couponId = couponIdByUser.get(uid);
                Order order = orderFacade.placeOrder(uid, commands, couponId);
                assertThat(order).isNotNull();
                success.incrementAndGet();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                done.countDown();
            }
        }));

        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        if (!errors.isEmpty()) {
            errors.forEach(e -> System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage()));
        }

        assertThat(success.get()).isEqualTo(threads);

        productRepository.findAll().forEach(p ->
                assertThat(p.getStock()).as("stock of " + p.getName()).isGreaterThanOrEqualTo(0));

        int expectedSold = threads * 6;
        int initialTotal = 5 * 10;
        int currentTotal = productRepository.findAll().stream().mapToInt(Product::getStock).sum();
        int realSold = initialTotal - currentTotal;
        assertThat(realSold).isEqualTo(expectedSold);
    }
}
