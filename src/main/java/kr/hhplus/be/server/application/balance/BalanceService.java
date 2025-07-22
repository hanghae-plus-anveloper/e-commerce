package kr.hhplus.be.server.application.balance;

import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.domain.balance.BalanceRepository;
import kr.hhplus.be.server.exception.UserNotFoundException;
import kr.hhplus.be.server.exception.InsufficientBalanceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final UserRepository userRepository;
    private final BalanceRepository balanceRepository;

    @Transactional
    public void chargeBalance(Long userId, int amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        Balance balance = user.getBalance();
        if (balance == null) {
            balance = new Balance(user, 0);
            user.setBalance(balance); // 연관관계 설정
        }

        balance.charge(amount);
        balanceRepository.save(balance);
    }

    @Transactional
    public void useBalance(Long userId, int amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        Balance balance = user.getBalance();
        if (balance == null || balance.getBalance() < amount) {
            throw new InsufficientBalanceException("잔액이 부족합니다.");
        }

        balance.use(amount);
        balanceRepository.save(balance);
    }
}
