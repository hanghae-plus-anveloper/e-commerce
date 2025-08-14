package kr.hhplus.be.server.coupon.facade;

import kr.hhplus.be.server.IntegrationTestContainersConfig;
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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(IntegrationTestContainersConfig.class)
public class CouponFacadeConcurrencyTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponFacade couponFacade;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();
        couponPolicyRepository.deleteAll();
        userRepository.deleteAll();

    }

    @Test
    @DisplayName("동시에 쿠폰 발급을 시도해도 초과 발급되지 않는다.")
    void issueCoupon_concurrently() throws InterruptedException {
        for (int i = 1; i <= 20; i++) {
            userRepository.save(User.builder().name("user-" + i).build());
        }

        int remainingCount = 10;

        CouponPolicy policy = couponPolicyRepository.save(
                CouponPolicy.builder()
                        .discountAmount(1000)
                        .discountRate(0.0)
                        .availableCount(remainingCount)
                        .remainingCount(remainingCount)
                        .expireDays(30)
                        .startedAt(LocalDateTime.now().minusDays(1))
                        .endedAt(LocalDateTime.now().plusDays(1))
                        .build());

        List<User> users = userRepository.findAll();

        ExecutorService executor = Executors.newFixedThreadPool(users.size());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(users.size());

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (User user : users) {
            executor.submit(() -> {
                try {
                    start.await(); // 동시에 시작
                    couponFacade.issueCoupon(user.getId(), policy.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.printf("실패 userId:\t%d\t(%s)%n", user.getId(), e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS); // 완전 종료 대기

        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("DB 저장된 쿠폰 수: " + couponRepository.count());

        assertThat(successCount.get()).isEqualTo(remainingCount);
    }
}
