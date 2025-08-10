package kr.hhplus.be.server.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.common.dto.CustomErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "PRODUCT API", description = "상품 관련 API 입니다.")
@RequestMapping("/products")
public interface ProductApi {

    @Operation(summary = "전체 상품 목록 조회", description = "전체 상품 목록과 재고 수량을 반환합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ProductResponseDto.class))
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "상품이 존재하지 않음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomErrorResponse.class),
                examples = @ExampleObject(
                    name = "상품 없음 예시",
                    value = """
                    {
                      "message": "상품을 찾을 수 없습니다.",
                      "status": 404
                    }
                    """
                )
            )
        )
    })
    @GetMapping
    ResponseEntity<List<ProductResponseDto>> getAllProducts();

    @Operation(summary = "단일 상품 조회", description = "상품 ID로 단일 상품을 조회합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ProductResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "상품이 존재하지 않음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomErrorResponse.class),
                examples = @ExampleObject(
                    name = "상품 없음 예시",
                    value = """
                    {
                      "message": "상품을 찾을 수 없습니다.",
                      "status": 404
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/{productId}")
    ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long productId);
}
