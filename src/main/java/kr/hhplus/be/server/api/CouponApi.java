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
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CouponResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "잘못된 사용자 ID",
                        summary = "userId가 0 이하",
                        value = """
                        {
                          "message": "userId는 1 이상이어야 합니다.",
                          "status": 400
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "쿠폰 ID 누락",
                        summary = "couponId가 없음",
                        value = """
                        {
                          "message": "couponId는 필수입니다.",
                          "status": 400
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "쿠폰 소진",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomErrorResponse.class),
                examples = @ExampleObject(
                    name = "쿠폰 소진 예시",
                    summary = "쿠폰 소진",
                    description = "모든 쿠폰이 이미 발급된 경우",
                    value = """
                    {
                      "message": "쿠폰이 모두 소진되었습니다.",
                      "status": 409
                    }
                    """
                )

            )
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
            responseCode = "404",
            description = "사용자 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomErrorResponse.class),
                examples = @ExampleObject(
                    name = "사용자 없음 예시",
                    value = """
                    {
                      "message": "사용자를 찾을 수 없습니다.",
                      "status": 404
                    }
                    """
                )
            )
        )
    })

    @GetMapping("/users/{userId}/coupons")
    ResponseEntity<List<CouponResponseDto>> getUserCoupons(@PathVariable @Min(1) Long userId);
}
