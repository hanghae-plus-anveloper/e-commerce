package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.api.CouponApi;
import kr.hhplus.be.server.dto.CouponResponseDto;
import kr.hhplus.be.server.exception.CouponSoldOutException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CouponController implements CouponApi {

    @Override
    public ResponseEntity<CouponResponseDto> claimCoupon(Long userId) {
        boolean couponsExhausted = true;
        if (couponsExhausted) {
            throw new CouponSoldOutException("쿠폰이 모두 소진되었습니다.");
        }

        CouponResponseDto coupon = new CouponResponseDto(1L, 1000);
        return ResponseEntity.status(201).body(coupon);
    }

    @Override
    public ResponseEntity<List<CouponResponseDto>> getUserCoupons(Long userId) {
        List<CouponResponseDto> coupons = List.of(
                new CouponResponseDto(1L, 1000)
        );
        return ResponseEntity.ok(coupons);
    }
}
