package kr.hhplus.be.server.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    @Override
    public ResponseEntity<BalanceResponseDto> getBalance(Long userId) {
        return ResponseEntity.ok(new BalanceResponseDto(userId, 5000));
    }

    @Override
    public ResponseEntity<BalanceResponseDto> chargeBalance(Long userId, ChargeRequestDto request) {
        if (userId <= 0 || request.getAmount() < 1) {
            return ResponseEntity.badRequest().build();
        }

        int currentBalance = 1500;
        int newBalance = currentBalance + request.getAmount();

        return ResponseEntity.status(201)
                .body(new BalanceResponseDto(userId, newBalance));
    }
}
