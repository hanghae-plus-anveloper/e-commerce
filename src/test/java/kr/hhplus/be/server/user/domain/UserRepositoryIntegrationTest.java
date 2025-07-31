package kr.hhplus.be.server.user.domain;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
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
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class) // 생략해도 동작
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BalanceRepository balanceRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();
        couponPolicyRepository.deleteAll();
        balanceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("사용자를 저장하면 ID가 생성된다")
    void saveUser() {
        User user = User.builder().name("성진").build();
        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("성진");
    }

    @Test
    @DisplayName("사용자와 Balance는 OneToOne 관계로 저장된다")
    void saveUserWithBalance() {
        User user = User.builder().name("성진").build();
        Balance balance = Balance.builder().user(user).balance(10000).build();
        user.setBalance(balance);

        userRepository.save(user);

        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getBalance()).isNotNull();
        assertThat(found.getBalance().getBalance()).isEqualTo(10000);
    }

    @Test
    @DisplayName("사용자와 Coupon은 OneToMany 관계로 연결된다")
    @Transactional
    void saveUserWithCoupons() {
        User user = userRepository.save(User.builder().name("성진").build());

        CouponPolicy policy = couponPolicyRepository.save(
                CouponPolicy.builder()
                        .discountAmount(1000)
                        .availableCount(10)
                        .remainingCount(10)
                        .expireDays(7)
                        .startedAt(java.time.LocalDateTime.now().minusDays(1))
                        .endedAt(java.time.LocalDateTime.now().plusDays(1))
                        .build()
        );

        Coupon c1 = Coupon.builder().user(user).policy(policy).discountAmount(1000).build();
        Coupon c2 = Coupon.builder().user(user).policy(policy).discountAmount(2000).build();

        user.getCoupons().add(c1); // DataJpaTest에선 없이도 성공
        user.getCoupons().add(c2);

        couponRepository.saveAll(List.of(c1, c2));

        User found = userRepository.findById(user.getId()).orElseThrow(); // add 추가 후, find로 다시 조회해도 조회 성공
        assertThat(found.getCoupons()).hasSize(2);
    }
}
