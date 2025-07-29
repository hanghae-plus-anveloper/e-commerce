package kr.hhplus.be.server.balance.domain;

import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class BalanceRepositoryTest {

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자 ID로 잔액 정보를 조회할 수 있다")
    void findByUserId() {
        User user = userRepository.save(User.builder().name("성진").build());

        Balance balance = Balance.builder()
                .user(user)
                .balance(10000)
                .build();
        balanceRepository.save(balance);

        Optional<Balance> result = balanceRepository.findByUserId(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getBalance()).isEqualTo(10000);
        assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
    }


    @Test
    @DisplayName("잔액을 사용할 수 있다")
    void useBalanceSuccess() {
        User user = userRepository.save(User.builder().name("성진").build());

        Balance balance = Balance.builder()
                .user(user)
                .balance(10000)
                .build();
        balance.use(4000);
        balanceRepository.save(balance);

        Optional<Balance> result = balanceRepository.findByUserId(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getBalance()).isEqualTo(6000);
    }

    @Test
    @DisplayName("잔액 부족 시 사용에 실패한다")
    void useBalanceFail() {
        User user = userRepository.save(User.builder().name("성진").build());

        Balance balance = Balance.builder()
                .user(user)
                .balance(3000)
                .build();

        assertThatThrownBy(() -> balance.use(5000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔액 부족");
    }
}
