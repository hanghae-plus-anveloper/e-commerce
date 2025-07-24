package kr.hhplus.be.server.controller.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.exception.CouponSoldOutException;
import kr.hhplus.be.server.facade.user.UserFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CouponController implements CouponApi {

    private final UserFacade userFacade;

    @Override
    public ResponseEntity<CouponResponseDto> claimCoupon(Long userId, Long policyId) {
        Coupon coupon = userFacade.issueCoupon(userId, policyId);

        CouponResponseDto dto = new CouponResponseDto(coupon.getId(), coupon.getDiscountAmount());
        return ResponseEntity.status(201).body(dto);
    }

    @Override
    public ResponseEntity<List<CouponResponseDto>> getUserCoupons(Long userId) {
        List<Coupon> coupons = userFacade.getCoupons(userId);
        List<CouponResponseDto> result = coupons.stream()
                .map(c -> new CouponResponseDto(c.getId(), c.getDiscountAmount()))
                .toList();
        return ResponseEntity.ok(result);
    }
}
