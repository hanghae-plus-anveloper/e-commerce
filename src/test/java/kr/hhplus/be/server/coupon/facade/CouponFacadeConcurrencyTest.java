package kr.hhplus.be.server.coupon.facade;

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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CouponFacadeConcurrencyTest {

    @Autowired
    private CouponFacade couponFacade;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private CouponPolicyRepository couponPolicyRepository;
    @Autowired
    private UserRepository userRepository;

    private CouponPolicy policy;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();
        userRepository.deleteAll();
        couponPolicyRepository.deleteAll();

        policy = couponPolicyRepository.save(CouponPolicy.builder()
                .discountAmount(1000)
                .availableCount(3) // 발급 수량 3개
                .remainingCount(3)
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .expireDays(30)
                .build());
    }

    @Test
    @DisplayName("동시에 여러 유저가 쿠폰을 발급받더라도 초과 발급되지 않는다")
    void issueCoupon_concurrently_limit_not_exceed() throws InterruptedException {
        int threadCount = 10; // 10명의 스레드 생성
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<CompletableFuture<Void>> futures = IntStream.rangeClosed(1, threadCount).mapToObj(i -> CompletableFuture.runAsync(() -> {
            try {
                User user = userRepository.save(User.builder().name("user-" + i).build());

                couponFacade.issueCoupon(user.getId(), policy.getId());
            } catch (Exception e) {
                // e.printStackTrace();
                // fail("error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        }, executor)).toList();

        latch.await();

        List<Coupon> issued = couponRepository.findAllWithUser();
        issued.forEach(c -> System.out.println("발급된 쿠폰: " + c.getId() + ", 사용자: " + c.getUser().getName()));

        System.out.println("발급된 쿠폰 수: " + issued.size());
        assertThat(issued).hasSize(3); // 발급 수량 초과되지 않아야 함
    }
}
