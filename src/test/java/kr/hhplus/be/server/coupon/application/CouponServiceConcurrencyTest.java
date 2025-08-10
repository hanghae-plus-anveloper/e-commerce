package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CouponServiceConcurrencyTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponService couponService;

    private Coupon issuedCoupon;
    private User user;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();
        userRepository.deleteAll();
        couponPolicyRepository.deleteAll();

        user = userRepository.save(User.builder().name("test-user").build());

        CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.builder()
                .discountAmount(1000)
                .availableCount(1)
                .remainingCount(1)
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .expireDays(30)
                .build());

        issuedCoupon = couponRepository.save(Coupon.builder()
                .policy(policy)
                .user(user)
                .discountAmount(policy.getDiscountAmount())
                .discountRate(policy.getDiscountRate())
                .used(false)
                .build());
    }

    @Test
    @DisplayName("동시에 동일한 쿠폰을 사용하려고 할 때 중복 사용이 발생하지 않아야 한다")
    void useCoupon_concurrently_only_once_used() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        ConcurrentMap<String, AtomicInteger> exceptionMap = new ConcurrentHashMap<>();

        for (int i = 0; i < threadCount; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    couponService.useCoupon(issuedCoupon.getId(), user.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptionMap
                            .computeIfAbsent(e.getClass().getSimpleName(), key -> new AtomicInteger(0))
                            .incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, executor);
        }

        latch.await();
        executor.shutdown();

        Coupon result = couponRepository.findById(issuedCoupon.getId()).orElseThrow();


        System.out.println("최종 used 상태: " + result.isUsed());
        System.out.println("성공한 사용 요청 수: " + successCount.get());
        System.out.println("실패한 예외 현황:");
        for (Map.Entry<String, AtomicInteger> entry : exceptionMap.entrySet()) {
            System.out.println(" - " + entry.getKey() + ": " + entry.getValue().get());
        }

        assertThat(result.isUsed()).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
    }
}
