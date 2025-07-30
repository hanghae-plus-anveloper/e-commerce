package kr.hhplus.be.server.coupon.domain;

import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자의 쿠폰 목록을 조회할 수 있다")
    void findAllByUserId() {
        User user = userRepository.save(User.builder().name("테스트유저").build());
        CouponPolicy policy = couponPolicyRepository.save(createValidPolicy());

        couponRepository.save(Coupon.builder()
                .policy(policy)
                .user(user)
                .discountAmount(1000)
                .discountRate(0.0)
                .used(false)
                .build());

        List<Coupon> result = couponRepository.findAllByUserId(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("사용자 ID와 쿠폰 ID로 쿠폰을 조회할 수 있다")
    void findByIdAndUserId() {
        User user = userRepository.save(User.builder().name("테스트유저").build());
        CouponPolicy policy = couponPolicyRepository.save(createValidPolicy());

        Coupon saved = couponRepository.save(Coupon.builder()
                .policy(policy)
                .user(user)
                .discountAmount(2000)
                .discountRate(0.0)
                .used(false)
                .build());

        Optional<Coupon> result = couponRepository.findByIdAndUserId(saved.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getDiscountAmount()).isEqualTo(2000);
    }

    private CouponPolicy createValidPolicy() {
        return CouponPolicy.builder()
                .discountAmount(1000)
                .discountRate(0.0)
                .availableCount(10)
                .remainingCount(10)
                .expireDays(7)
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();
    }
}
