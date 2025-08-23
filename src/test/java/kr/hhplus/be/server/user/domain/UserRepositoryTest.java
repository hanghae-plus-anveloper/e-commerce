package kr.hhplus.be.server.user.domain;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

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
    void saveUserWithCoupons() {
        User user = userRepository.save(User.builder().name("성진").build());

        CouponPolicy policy = couponPolicyRepository.save(
                CouponPolicy.builder()
                        .discountAmount(1000)
                        .build()
        );

        Coupon c1 = Coupon.builder().userId(user.getId()).user(user).policy(policy).discountAmount(1000).build();
        Coupon c2 = Coupon.builder().userId(user.getId()).user(user).policy(policy).discountAmount(2000).build();

        c1.setUser(user);
        c2.setUser(user);
        user.getCoupons().addAll(List.of(c1, c2)); // List<Coupon> coupons = new ArrayList<>(); 초기화 필요

        couponRepository.saveAll(java.util.List.of(c1, c2));

        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getCoupons()).hasSize(2);
    }
}
