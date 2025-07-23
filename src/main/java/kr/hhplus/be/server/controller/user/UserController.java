package kr.hhplus.be.server.controller.user;

import kr.hhplus.be.server.facade.user.UserFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserFacade userFacade;

    @Override
    public ResponseEntity<BalanceResponseDto> getBalance(Long userId) {
        BalanceResponseDto balance = userFacade.getBalance(userId);
        return ResponseEntity.ok(balance);
    }

    @Override
    public ResponseEntity<BalanceResponseDto> chargeBalance(Long userId, ChargeRequestDto request) {
        userFacade.chargeBalance(userId, request.getAmount());
        BalanceResponseDto balance = userFacade.getBalance(userId);
        return ResponseEntity.status(201).body(balance);
    }
}
