package kr.hhplus.be.server.application.balance;


import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.balance.BalanceChangeType;
import kr.hhplus.be.server.domain.balance.BalanceRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BalanceServiceTest {
    private BalanceService balanceService;
    private BalanceRepository balanceRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        balanceRepository = mock(BalanceRepository.class);
        userRepository = mock(UserRepository.class);
        balanceService = new BalanceService(balanceRepository);
    }

    @Test
    @DisplayName("잔액 충전 시 잔액이 증가하고 히스토리가 기록된다")
    void chargeBalance() {
        User user = new User("테스트유저");
        user.setId(1L);
        Balance balance = new Balance(user, 1000);
        user.setBalance(balance);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        balanceService.chargeBalance(user, 500);

        assertThat(user.getBalance().getBalance()).isEqualTo(1500);
        assertThat(user.getBalance().getHistories()).hasSize(1);
        assertThat(user.getBalance().getHistories().get(0).getType()).isEqualTo(BalanceChangeType.CHARGE);
    }

    @Test
    @DisplayName("잔액 사용 시 잔액이 감소하고 히스토리가 기록된다")
    void useBalance() {
        User user = new User("테스트유저");
        user.setId(1L);
        Balance balance = new Balance(user, 1000);
        user.setBalance(balance);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        balanceService.useBalance(user, 300);

        assertThat(user.getBalance().getBalance()).isEqualTo(700);
        assertThat(user.getBalance().getHistories()).hasSize(1);
        assertThat(user.getBalance().getHistories().get(0).getType()).isEqualTo(BalanceChangeType.USE);
    }

    @Test
    @DisplayName("잔액 부족 시 IllegalArgumentException이 발생한다")
    void useBalance_insufficient() {
        User user = new User("테스트유저");
        user.setId(1L);
        Balance balance = new Balance(user, 100);
        user.setBalance(balance);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> balanceService.useBalance(user, 300))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("잔액이 부족합니다.");
    }
}
