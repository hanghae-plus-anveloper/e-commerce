package kr.hhplus.be.server.balance.domain;

import kr.hhplus.be.server.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceTest {

    @Test
    @DisplayName("잔액을 충전하면 잔액이 증가하고 히스토리가 생성된다")
    void chargeBalance() {
        User user = User.builder().name("성진").build();
        Balance balance = Balance.builder()
                .user(user)
                .balance(0)
                .build();

        balance.charge(5000);

        assertThat(balance.getBalance()).isEqualTo(5000);
        assertThat(balance.getHistories()).hasSize(1);
        assertThat(balance.getHistories().get(0).getAmount()).isEqualTo(5000);
        assertThat(balance.getHistories().get(0).getType()).isEqualTo(BalanceChangeType.CHARGE);
    }

    @Test
    @DisplayName("잔액을 사용하면 잔액이 감소하고 히스토리가 생성된다")
    void useBalance() {
        User user = User.builder().name("성진").build();
        Balance balance = Balance.builder()
                .user(user)
                .balance(10000)
                .build();

        balance.use(4000);

        assertThat(balance.getBalance()).isEqualTo(6000);
        assertThat(balance.getHistories()).hasSize(1);
        assertThat(balance.getHistories().get(0).getAmount()).isEqualTo(-4000);
        assertThat(balance.getHistories().get(0).getType()).isEqualTo(BalanceChangeType.USE);
    }

    @Test
    @DisplayName("사용 금액이 잔액보다 크면 예외가 발생한다")
    void useBalanceFailWhenInsufficient() {
        User user = User.builder().name("성진").build();
        Balance balance = Balance.builder()
                .user(user)
                .balance(1000)
                .build();

        assertThatThrownBy(() -> balance.use(2000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액 부족");

        assertThat(balance.getBalance()).isEqualTo(1000);
        assertThat(balance.getHistories()).isEmpty();
    }
}
