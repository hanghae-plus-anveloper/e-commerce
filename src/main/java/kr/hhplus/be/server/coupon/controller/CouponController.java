package kr.hhplus.be.server.coupon.controller;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.facade.CouponFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CouponController implements CouponApi {

    private final CouponFacade couponFacade;

    @Override
    public ResponseEntity<CouponResponseDto> claimCoupon(Long userId, Long policyId) {
        Coupon coupon = couponFacade.issueCoupon(userId, policyId);

        CouponResponseDto dto = new CouponResponseDto(coupon.getId(), coupon.getDiscountAmount());
        return ResponseEntity.status(201).body(dto);
    }

    @Override
    public ResponseEntity<List<CouponResponseDto>> getUserCoupons(Long userId) {
        List<Coupon> coupons = couponFacade.getCoupons(userId);
        List<CouponResponseDto> result = coupons.stream()
                .map(c -> new CouponResponseDto(c.getId(), c.getDiscountAmount()))
                .toList();
        return ResponseEntity.ok(result);
    }
}
