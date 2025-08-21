package kr.hhplus.be.server.coupon.domain;

import kr.hhplus.be.server.IntegrationTestContainersConfig;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Import(IntegrationTestContainersConfig.class)
class CouponRepositoryIntegrationTest {

    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponPolicyRepository couponPolicyRepository;
    @Autowired private UserRepository userRepository;

    private User user;
    private CouponPolicy policy;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();
        couponPolicyRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("통합테스트 유저"));
        policy = couponPolicyRepository.save(CouponPolicy.builder()
                .discountAmount(3000)
                .discountRate(0.0)
                .availableCount(5)
                .remainingCount(5)
                .expireDays(3)
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build());

        couponRepository.save(Coupon.builder()
                .policy(policy)
                .userId(user.getId())
                .user(user)
                .discountAmount(3000)
                .discountRate(0.0)
                .used(false)
                .build());
    }

    @Test
    @DisplayName("사용자의 쿠폰을 전부 조회할 수 있다")
    void findAllByUserId() {
        List<Coupon> result = couponRepository.findAllByUserId(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @Transactional // FetchType.LAZY 관련 필드 접근 시 세션 죽는 문제, 영속성 유지가 필요함
    @DisplayName("쿠폰 ID + 사용자 ID로 하나만 조회할 수 있다")
    void findByIdAndUserId() {
        Long couponId = couponRepository.findAll().get(0).getId();
        Optional<Coupon> result = couponRepository.findByIdAndUserId(couponId, user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getPolicy().getDiscountAmount()).isEqualTo(3000);
    }
}
