package kr.hhplus.be.server.controller.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "상위 인기 상품 응답 DTO")
public class TopProductResponseDto {

    @Schema(description = "상품 ID", example = "101")
    private Long productId;

    @Schema(description = "상품명", example = "프리미엄 물티슈")
    private String name;

    @Schema(description = "가격", example = "1500")
    private int price;

    @Schema(description = "현재 재고", example = "25")
    private int stock;

    @Schema(description = "최근 3일간 판매량", example = "300")
    private int soldCount;
}
