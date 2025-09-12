package kr.hhplus.be.server.coupon.controller;

import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CouponController implements CouponApi {

    private final CouponService couponService;

    @Override
    public ResponseEntity<CouponResponseDto> claimCoupon(Long userId, Long policyId) {
        Coupon coupon = couponService.issueCoupon(userId, policyId);

        CouponResponseDto dto = new CouponResponseDto(coupon.getId(), coupon.getDiscountAmount());
        return ResponseEntity.status(201).body(dto);
    }

    @Override
    public ResponseEntity<List<CouponResponseDto>> getUserCoupons(Long userId) {
        List<Coupon> coupons = couponService.getCoupons(userId);
        List<CouponResponseDto> result = coupons.stream()
                .map(c -> new CouponResponseDto(c.getId(), c.getDiscountAmount()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<String> tryClaimCoupon(@RequestParam Long userId,
                                                 @RequestParam Long policyId) {
        boolean accepted = couponService.tryIssue(userId, policyId);
        if (accepted) {
            return ResponseEntity.accepted().body("쿠폰 발급 요청이 접수되었습니다.");
        } else {
            return ResponseEntity.status(409).body("쿠폰이 모두 소진되었습니다.");
        }
    }
}
