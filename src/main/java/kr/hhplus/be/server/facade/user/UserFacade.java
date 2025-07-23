package kr.hhplus.be.server.facade.user;

import kr.hhplus.be.server.application.balance.BalanceService;
import kr.hhplus.be.server.application.user.UserService;
import kr.hhplus.be.server.controller.user.BalanceResponseDto;
import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.user.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserFacade {

    private final UserService userService;
    private final BalanceService balanceService;

    public UserFacade(UserService userService, BalanceService balanceService) {
        this.userService = userService;
        this.balanceService = balanceService;
    }

    @Transactional
    public void chargeBalance(Long userId, int amount) {
        User user = userService.findById(userId);
        balanceService.chargeBalance(user, amount);
    }

    @Transactional
    public void useBalance(Long userId, int amount) {
        User user = userService.findById(userId);
        balanceService.useBalance(user, amount);
    }

    @Transactional(readOnly = true)
    public BalanceResponseDto getBalance(Long userId) {
        User user = userService.findById(userId);
        return new BalanceResponseDto(user.getId(), user.getBalance().getBalance());
    }
}