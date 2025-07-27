package kr.hhplus.be.server.controller.balance;

import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.facade.balance.BalanceFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BalanceController implements BalanceApi {

    private final BalanceFacade balanceFacade;

    @Override
    public ResponseEntity<BalanceResponseDto> getBalance(Long userId) {
        Balance balance = balanceFacade.getBalance(userId);
        return ResponseEntity.ok(new BalanceResponseDto(userId, balance.getBalance()));
    }

    @Override
    public ResponseEntity<BalanceResponseDto> chargeBalance(Long userId, ChargeRequestDto request) {
        balanceFacade.chargeBalance(userId, request.getAmount());
        Balance balance = balanceFacade.getBalance(userId);
        return ResponseEntity.status(201).body(new BalanceResponseDto(userId, balance.getBalance()));
    }
}
