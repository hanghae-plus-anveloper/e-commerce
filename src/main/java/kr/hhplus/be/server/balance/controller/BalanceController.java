package kr.hhplus.be.server.balance.controller;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.facade.BalanceFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BalanceController implements BalanceApi {

    private final BalanceFacade userFacade;

    @Override
    public ResponseEntity<BalanceResponseDto> getBalance(Long userId) {
        Balance balance = userFacade.getBalance(userId);
        return ResponseEntity.ok(new BalanceResponseDto(userId, balance.getBalance()));
    }

    @Override
    public ResponseEntity<BalanceResponseDto> chargeBalance(Long userId, ChargeRequestDto request) {
        userFacade.chargeBalance(userId, request.getAmount());
        Balance balance = userFacade.getBalance(userId);
        return ResponseEntity.status(201).body(new BalanceResponseDto(userId, balance.getBalance()));
    }
}
