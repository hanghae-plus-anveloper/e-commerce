package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.domain.*;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.balance.exception.InsufficientBalanceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    @Transactional
    public void chargeBalance(User user, int amount) {

        Balance balance = balanceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Balance newBalance = new Balance(user, 0);
                    user.setBalance(newBalance);
                    return balanceRepository.save(newBalance);
                });

        balance.charge(amount);
        balanceRepository.save(balance);

        BalanceHistory history = new BalanceHistory(
                balance,
                amount,
                balance.getBalance(),
                BalanceChangeType.CHARGE
        );
        balanceHistoryRepository.save(history);
    }

    @Transactional
    public void useBalance(User user, int amount) {

        Balance balance = balanceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("잔액 정보 없음"));

        if (balance == null || balance.getBalance() < amount) {
            throw new InsufficientBalanceException("잔액이 부족합니다.");
        }

        balance.use(amount);
        balanceRepository.save(balance);

        BalanceHistory history = new BalanceHistory(
                balance,
                -amount,
                balance.getBalance(),
                BalanceChangeType.USE
        );
        balanceHistoryRepository.save(history);
    }
}
