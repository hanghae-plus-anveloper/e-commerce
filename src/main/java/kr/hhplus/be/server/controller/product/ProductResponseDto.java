package kr.hhplus.be.server.controller.product;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.domain.product.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "상품 정보 응답 DTO")
public class ProductResponseDto {
    @Schema(description = "상품 ID", example = "1")
    private Long id;

    @Schema(description = "상품 이름", example = "USB-C 충전기")
    private String name;

    @Schema(description = "상품 가격", example = "19900")
    private int price;

    @Schema(description = "잔여 수량", example = "25")
    private int stock;

    public static ProductResponseDto from(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock()
        );
    }
}
