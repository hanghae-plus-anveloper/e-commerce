package kr.hhplus.be.server.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "상품 통계 API", description = "최근 판매량 기준 상위 상품 조회 API")
@RequestMapping("/products")
public interface ProductStatisticsApi {

    @Operation(summary = "상위 인기 상품 조회", description = "최근 3일간 판매량 기준으로 인기 상품 5개를 조회합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TopProductResponseDto.class)))
        )
    })
    @GetMapping("/top")
    ResponseEntity<List<TopProductResponseDto>> getTopProducts();
}
