package kr.hhplus.be.server.boot;

import kr.hhplus.be.server.analytics.application.TopProductService;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import kr.hhplus.be.server.coupon.infrastructure.CouponRedisRepository;
import kr.hhplus.be.server.order.domain.OrderItemRepository;
import kr.hhplus.be.server.order.domain.OrderRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class BootDataInitializer implements ApplicationRunner {

    private static final int USER_COUNT = 300;

    private static final int PRODUCT_COUNT = 5;
    private static final int PRODUCT_STOCK = 2_000_000;
    private static final int PRODUCT_PRICE = 2_000;

    private static final int COUPON_AVAILABLE = 100_000;
    private static final int COUPON_REMAINING = 100_000;
    private static final int COUPON_DISCOUNT_AMOUNT = 2_000;
    private static final double COUPON_DISCOUNT_RATE = 0.0;
    private static final int COUPON_EXPIRE_DAYS = 7;

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private final CouponRedisRepository couponRedisRepository;
    private final TopProductService topProductService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== [local] Boot Data Initializer: start ===");

        clearData();

        List<User> users = seedUsers();
        List<Product> products = seedProducts();
        CouponPolicy policy = seedCouponPolicy();
        seedTop5Last3Days(products);

        log.info("=== [local] Boot Data Initializer: done (users={}, policyId={}, products={}) ===",
                users.size(), policy.getId(), products.stream().map(Product::getId).toList());
    }

    @Transactional
    public CouponPolicy runAndReturnPolicy() throws Exception {
        log.info("=== [local] Manual Boot Data Initializer: start ===");

        clearData();

        List<User> users = seedUsers();
        List<Product> products = seedProducts();
        CouponPolicy policy = seedCouponPolicy();
        seedTop5Last3Days(products);

        log.info("=== [local] Manual Boot Data Initializer: done (users={}, policyId={}, products={}) ===",
                users.size(), policy.getId(), products.stream().map(Product::getId).toList());
        return policy;
    }

    private void clearData() {
        log.info("[INIT] clearing all existing data...");

        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();

        couponRepository.deleteAllInBatch();
        couponPolicyRepository.deleteAllInBatch();

        productRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        try {
            couponRedisRepository.removePolicyAll();
            topProductService.clearAll();
        } catch (Exception e) {
            log.warn("[INIT][Redis] clear failed: {}", e.getMessage(), e);
        }

        log.info("[INIT] all existing data cleared");
    }

    private List<User> seedUsers() {
        List<User> batch = new ArrayList<>(USER_COUNT);
        for (int i = 1; i <= USER_COUNT; i++) {
            batch.add(User.builder().name("u" + i).build());
        }
        List<User> saved = userRepository.saveAll(batch);
        log.info("[INIT] seeded users: {}", saved.size());
        return saved;
    }

    // 기본 상품 세팅
    private List<Product> seedProducts() {
        List<Product> batch = new ArrayList<>(PRODUCT_COUNT);
        for (int i = 1; i <= PRODUCT_COUNT; i++) {
            Product p = Product.builder().name("p" + i).price(PRODUCT_PRICE).stock(PRODUCT_STOCK).build();
            batch.add(p);
        }
        List<Product> saved = productRepository.saveAll(batch);
        log.info("[INIT] seeded products: {}", saved.stream().map(Product::getId).toList());
        return saved;
    }

    // 쿠폰 정책 세팅
    private CouponPolicy seedCouponPolicy() {
        LocalDateTime now = LocalDateTime.now();
        CouponPolicy policy = CouponPolicy.builder().discountAmount(COUPON_DISCOUNT_AMOUNT).discountRate(COUPON_DISCOUNT_RATE).availableCount(COUPON_AVAILABLE).remainingCount(COUPON_REMAINING).expireDays(COUPON_EXPIRE_DAYS).startedAt(now.minusMinutes(1)).endedAt(now.plusDays(3)).build();
        policy = couponPolicyRepository.save(policy);

        try {
            couponRedisRepository.removePolicy(policy.getId());
            couponRedisRepository.setRemainingCount(policy.getId(), policy.getRemainingCount());
            log.info("[INIT][Redis] COUPON:POLICY:{}:* reset (remaining={})", policy.getId(), policy.getRemainingCount());
        } catch (Exception e) {
            log.warn("[INIT][Redis] coupon remaining reset failed: {}", e.getMessage(), e);
        }

        log.info("[INIT] seeded coupon policy: id={}, remaining={}", policy.getId(), policy.getRemainingCount());
        return policy;

    }

    // TOP 5 Redis 세팅
    private void seedTop5Last3Days(List<Product> products) {
        try {
            topProductService.clearAll();
        } catch (Exception e) {
            log.warn("[INIT][Redis] top-product clearAll failed: {}", e.getMessage(), e);
        }

        LocalDate today = LocalDate.now();
        int[] scores = {500, 400, 300, 200, 100};

        for (int d = 0; d < 3; d++) {
            LocalDate date = today.minusDays(d);
            for (int i = 0; i < products.size() && i < scores.length; i++) {
                Long pid = products.get(i).getId();
                topProductService.recordOrder(String.valueOf(pid), scores[i], date);
            }
        }
        log.info("[INIT][Redis] top-product last3days seeded");
    }
}
