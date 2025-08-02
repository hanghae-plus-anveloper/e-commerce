package kr.hhplus.be.server.order.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "주문 응답 DTO")
public class OrderResponseDto {
    @Schema(description = "주문 ID", example = "1001")
    private Long orderId;

    @Schema(description = "총 결제 금액", example = "20000")
    private int totalAmount;
}
