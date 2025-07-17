package kr.hhplus.be.server.api;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import kr.hhplus.be.server.dto.CouponResponseDto;
import kr.hhplus.be.server.dto.CustomErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "COUPON API", description = "선착순 쿠폰 관련 API 입니다.")
@RequestMapping
public interface CouponApi {

    @Operation(summary = "선착순 쿠폰 발급", description = "사용자에게 선착순으로 쿠폰을 발급합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "쿠폰 발급 성공",
            content = @Content(schema = @Schema(implementation = CouponResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "쿠폰 소진",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class))
        )
    })
    @PostMapping("/coupons")
    ResponseEntity<CouponResponseDto> claimCoupon(@RequestParam @Min(1) Long userId);

    @Operation(summary = "사용자 보유 쿠폰 조회", description = "특정 사용자가 보유한 쿠폰 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CouponResponseDto.class)))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 사용자 ID",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class))
        )
    })

    @GetMapping("/users/{userId}/coupons")
    ResponseEntity<List<CouponResponseDto>> getUserCoupons(@PathVariable @Min(1) Long userId);
}
