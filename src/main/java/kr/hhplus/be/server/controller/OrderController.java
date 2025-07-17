package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.api.OrderApi;
import kr.hhplus.be.server.dto.OrderRequestDto;
import kr.hhplus.be.server.dto.OrderResponseDto;
import kr.hhplus.be.server.exception.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController implements OrderApi {

    @Override
    public ResponseEntity<OrderResponseDto> createOrder(OrderRequestDto request) {
        // 예외 케이스 (mock 처리)
        if (request.getUserId() <= 0) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다.");
        }

        if (request.getCouponId() != null && request.getCouponId() <= 0) {
            throw new InvalidCouponException("쿠폰이 유효하지 않습니다.");
        }

        int totalAmount = 20000;

        // mock 잔액, 재고, 쿠폰 확인 생략
        if (totalAmount > 15000) {
            throw new InsufficientBalanceException("잔액이 부족합니다.");
        }

        return ResponseEntity.status(201).body(new OrderResponseDto(1001L, totalAmount));
    }
}
