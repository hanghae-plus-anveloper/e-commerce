package kr.hhplus.be.server.coupon.domain;

import kr.hhplus.be.server.coupon.exception.CouponSoldOutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class CouponPolicyTest {

    @Test
    @DisplayName("사용 가능 기간 내라면 true를 반환한다")
    void isWithinPeriod_returnsTrue() {
        CouponPolicy policy = CouponPolicy.builder()
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build();

        assertThat(policy.isWithinPeriod()).isTrue();
    }

    @Test
    @DisplayName("사용 가능 기간 밖이라면 false를 반환한다")
    void isWithinPeriod_returnsFalse() {
        CouponPolicy policy = CouponPolicy.builder()
                .startedAt(LocalDateTime.now().minusDays(10))
                .endedAt(LocalDateTime.now().minusDays(5))
                .build();

        assertThat(policy.isWithinPeriod()).isFalse();
    }

    @Test
    @DisplayName("잔여 수량이 있다면 true를 반환한다")
    void hasRemainingCount_true() {
        CouponPolicy policy = CouponPolicy.builder()
                .remainingCount(3)
                .build();

        assertThat(policy.hasRemainingCount()).isTrue();
    }

    @Test
    @DisplayName("잔여 수량이 0이면 false를 반환한다")
    void hasRemainingCount_false() {
        CouponPolicy policy = CouponPolicy.builder()
                .remainingCount(0)
                .build();

        assertThat(policy.hasRemainingCount()).isFalse();
    }

    @Test
    @DisplayName("잔여 수량 감소 시 1 감소한다")
    void decreaseRemainingCount_success() {
        CouponPolicy policy = CouponPolicy.builder()
                .remainingCount(2)
                .build();

        policy.decreaseRemainingCount();

        assertThat(policy.getRemainingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("잔여 수량이 0이면 예외를 발생시킨다")
    void decreaseRemainingCount_fail() {
        CouponPolicy policy = CouponPolicy.builder()
                .remainingCount(0)
                .build();

        assertThatThrownBy(policy::decreaseRemainingCount)
                .isInstanceOf(CouponSoldOutException.class)
                .hasMessageContaining("남은 쿠폰 수량이 없습니다.");
    }
}
