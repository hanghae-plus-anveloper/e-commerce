package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "주문 항목 DTO")
public class OrderItemRequestDto {
    @Schema(description = "상품 ID", example = "101")
    @Min(value = 1, message = "유효하지 않은 상품 ID입니다.")
    private Long productId;

    @Schema(description = "수량", example = "2")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private int quantity;
}
