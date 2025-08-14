package kr.hhplus.be.server.analytics.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "상위 인기 상품 응답 DTO")
public record TopProductResponseDto(

        @Schema(description = "상품 ID", example = "1")
        Long productId,

        @Schema(description = "상품 이름", example = "무선 이어폰")
        String name,

        @Schema(description = "총 판매 수량", example = "150")
        Long soldQty
) {
    @Builder
    public TopProductResponseDto {}

    public static TopProductResponseDto fromView(kr.hhplus.be.server.analytics.domain.TopProductView view) {
        return new TopProductResponseDto(
                view.productId(),
                view.name(),
                view.soldQty()
        );
    }
}
