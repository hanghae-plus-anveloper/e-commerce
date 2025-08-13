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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        couponIdByUser.clear();

        IntStream.rangeClosed(1, 10).forEach(i -> {
            User u = userRepository.save(User.builder().name("user-" + i).build());
            balanceRepository.save(Balance.builder().user(u).balance(1_000_000).build());
        });

        // 더미 상품 생성, 재고 10
        IntStream.rangeClosed(1, 10).forEach(i -> {
            productRepository.save(Product.builder().name("product-" + i).price(100).stock(10).build());
        });

        // 유효한 쿠폰 정책 > 쿠폰에 의한 실패 배제, 사용만 통제
        CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.builder().discountRate(0.1).discountAmount(0).availableCount(10_000).remainingCount(10_000).startedAt(LocalDateTime.now().minusDays(1)).endedAt(LocalDateTime.now().plusDays(1)).expireDays(30).build());

        // 사용자별 쿠폰 사용 추가
        userRepository.findAll().forEach(u -> {
            Coupon c = couponService.issueCoupon(u, policy.getId());
            couponIdByUser.put(u.getId(), c.getId());
        });
    }

    @Test
    @DisplayName("재고가 10인 10개의 상품을 10명이서 5종류 씩 순서에 상관없이 2개씩 구매할때, 10개의 상품이 모두 정상적으로 판매된다.")
    void placeOrders_concurrently() throws InterruptedException {
        List<User> users = userRepository.findAll().stream().sorted(Comparator.comparing(User::getId)).toList();
        List<Product> products = productRepository.findAll().stream().sorted(Comparator.comparing(Product::getId)).toList();

        final int userCount = users.size();             // 사용자 수 10
        final int productCount = products.size();       // 상품 수 10
        final int picksPerUser = 5;                     // 사용자 별 5개 상품 선택
        final int qtyPerPick = 2;                       // 2개씩 구매,

        final int initialTotalStock = products.stream().mapToInt(Product::getStock).sum();
        assertThat(initialTotalStock).isEqualTo(100); // 재고 10개씩 총 100개

        Map<Long, List<OrderItemCommand>> carts = new LinkedHashMap<>();
        for (int u = 0; u < userCount; u++) {
            Long userId = users.get(u).getId();


            List<OrderItemCommand> original = new ArrayList<>();
            for (int k = 0; k < picksPerUser; k++) {
                int pIndex = (u + k) % productCount;
                Long productId = products.get(pIndex).getId();
                original.add(new OrderItemCommand(productId, qtyPerPick));
            }

            List<OrderItemCommand> shuffled = new ArrayList<>(original);
            Collections.shuffle(shuffled, new Random(userId));

            System.out.printf("사용자: %d,\t원본: [%s],\t랜덤: [%s]%n", userId, formatCart(original), formatCart(shuffled));

            carts.put(userId, shuffled);
        }

        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(userCount);

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
        boolean finished = done.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        if (!finished) {
            System.out.println("타임아웃으로 일부 작업이 완료되지 않았습니다.");
        }

        if (!errors.isEmpty()) {
            System.out.println("예외 발생 수: " + errors.size());
            errors.stream().limit(10).forEach(e -> System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage()));
        }

        assertThat(success.get()).as("주문 성공 수: %s", success.get()).isEqualTo(userCount);

        List<Product> after = productRepository.findAll().stream().sorted(Comparator.comparing(Product::getId)).toList();
        after.forEach(p -> System.out.println(p.getName() + "\t(id=" + p.getId() + ")\tstock=" + p.getStock()));

        int currentTotal = after.stream().mapToInt(Product::getStock).sum();
        int expectedSold = userCount * picksPerUser * qtyPerPick;
        int realSold = initialTotalStock - currentTotal;

        assertThat(realSold).as("총 판매 수량: %s, 기대치: %s", realSold, expectedSold).isEqualTo(expectedSold);

        assertThat(after).allSatisfy(p -> assertThat(p.getStock()).as("%s(id=%s) 최종 재고가 0이어야 합니다.", p.getName(), p.getId()).isEqualTo(0));
    }

    private static String formatCart(List<OrderItemCommand> list) {
        return list.stream().map(c -> String.format("%d x%d", c.getProductId(), c.getQuantity())).collect(Collectors.joining(", "));
    }
}
