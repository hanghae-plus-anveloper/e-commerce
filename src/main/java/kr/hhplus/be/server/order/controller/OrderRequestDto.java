package kr.hhplus.be.server.order.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "주문 요청 DTO")
public class OrderRequestDto {

    @Schema(description = "사용자 ID", example = "1")
    @Min(value = 1, message = "유효하지 않은 사용자 ID입니다.")
    private Long userId;

    @Schema(description = "주문 항목 리스트")
    @Valid
    private List<OrderItemRequestDto> items;

    @Schema(description = "쿠폰 ID (선택)", example = "5")
    private Long couponId;
}
