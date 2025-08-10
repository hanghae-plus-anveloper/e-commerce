package kr.hhplus.be.server.balance.facade;

import kr.hhplus.be.server.balance.application.BalanceService;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.user.domain.User;
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