package kr.hhplus.be.server.coupon.domain;

import kr.hhplus.be.server.coupon.exception.InvalidCouponException;
import kr.hhplus.be.server.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    private final User user = User.builder().name("테스트 사용자").build();

    @Test
    @DisplayName("정상적인 쿠폰은 사용 가능하다")
    void useAvailableCoupon() {
        CouponPolicy policy = CouponPolicy.builder()
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();

        Coupon coupon = Coupon.builder()
                .userId(user.getId())
                .user(user)
                .policy(policy)
                .used(false)
                .discountAmount(1000)
                .build();

        coupon.use();

        assertThat(coupon.isUsed()).isTrue();
    }

    @Test
    @DisplayName("이미 사용한 쿠폰은 사용할 수 없다")
    void useAlreadyUsedCoupon() {
        CouponPolicy policy = CouponPolicy.builder()
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();

        Coupon coupon = Coupon.builder()
                .userId(user.getId())
                .user(user)
                .policy(policy)
                .used(true)
                .discountRate(0.1)
                .build();

        assertThatThrownBy(coupon::use)
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("사용할 수 없는 쿠폰");
    }

    @Test
    @DisplayName("기간이 지난 쿠폰은 사용할 수 없다")
    void useExpiredCoupon() {
        CouponPolicy policy = CouponPolicy.builder()
                .startedAt(LocalDateTime.now().minusDays(5))
                .endedAt(LocalDateTime.now().minusDays(1))
                .build();

        Coupon coupon = Coupon.builder()
                .userId(user.getId())
                .user(user)
                .policy(policy)
                .used(false)
                .discountAmount(500)
                .build();

        assertThatThrownBy(coupon::use)
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("사용할 수 없는 쿠폰");
    }

    @Test
    @DisplayName("쿠폰이 유효하면 isAvailable이 true")
    void isAvailable_true() {
        CouponPolicy policy = CouponPolicy.builder()
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();

        Coupon coupon = Coupon.builder()
                .userId(user.getId())
                .user(user)
                .policy(policy)
                .used(false)
                .build();

        assertThat(coupon.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("쿠폰이 이미 사용되었거나 기간이 지나면 isAvailable이 false")
    void isAvailable_false() {
        CouponPolicy expiredPolicy = CouponPolicy.builder()
                .startedAt(LocalDateTime.now().minusDays(5))
                .endedAt(LocalDateTime.now().minusDays(1))
                .build();

        Coupon usedCoupon = Coupon.builder()
                .userId(user.getId())
                .user(user)
                .policy(expiredPolicy)
                .used(true)
                .build();

        assertThat(usedCoupon.isAvailable()).isFalse();
    }
}
