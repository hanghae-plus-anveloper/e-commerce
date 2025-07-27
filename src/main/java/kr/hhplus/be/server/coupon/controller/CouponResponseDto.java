package kr.hhplus.be.server.coupon.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "쿠폰 응답 DTO")
public class CouponResponseDto {

    @Schema(description = "쿠폰 ID", example = "1")
    private Long couponId;

    @Schema(description = "할인 금액", example = "1000")
    private int discount;
}
