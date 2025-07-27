package kr.hhplus.be.server.facade.balance;

import kr.hhplus.be.server.application.balance.BalanceService;
import kr.hhplus.be.server.application.user.UserService;
import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class BalanceFacade {

    private final UserService userService;
    private final BalanceService balanceService;

    @Transactional
    public void chargeBalance(Long userId, int amount) {
        User user = userService.findById(userId);
        balanceService.chargeBalance(user, amount);
    }

    @Transactional(readOnly = true)
    public Balance getBalance(Long userId) {
        User user = userService.findById(userId);
        return user.getBalance();
    }

}