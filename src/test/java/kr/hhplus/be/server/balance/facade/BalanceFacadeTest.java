package kr.hhplus.be.server.balance.facade;

import kr.hhplus.be.server.balance.application.BalanceService;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class BalanceFacadeTest {

    private UserService userService;
    private BalanceService balanceService;
    private BalanceFacade balanceFacade;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        balanceService = mock(BalanceService.class);
        balanceFacade = new BalanceFacade(userService, balanceService);
    }

    @Test
    @DisplayName("잔액 충전 시 BalanceService가 호출된다")
    void chargeBalance_success() {
        Long userId = 1L;
        int amount = 500;
        User user = new User("테스트 유저");
        Balance balance = new Balance(user, 1000);
        user.setBalance(balance);

        when(userService.findById(userId)).thenReturn(user);

        balanceFacade.chargeBalance(userId, amount);

        verify(userService, times(1)).findById(userId);
        verify(balanceService, times(1)).chargeBalance(user, amount);
    }

    @Test
    @DisplayName("잔액 조회 시 사용자 잔액을 반환한다")
    void getBalance_success() {
        Long userId = 1L;
        User user = new User("테스트 유저");
        Balance balance = new Balance(user, 3000);
        user.setBalance(balance);

        when(userService.findById(userId)).thenReturn(user);

        Balance result = balanceFacade.getBalance(userId);

        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualTo(3000);
        verify(userService, times(1)).findById(userId);
        verifyNoInteractions(balanceService);
    }
}
