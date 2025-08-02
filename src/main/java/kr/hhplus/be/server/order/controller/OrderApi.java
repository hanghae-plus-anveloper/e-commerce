package kr.hhplus.be.server.order.controller;

import kr.hhplus.be.server.common.dto.CustomErrorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "ORDER API", description = "주문 및 결제 API 입니다.")
@RequestMapping("/orders")
public interface OrderApi {

    @Operation(summary = "주문 생성 및 결제", description = "상품을 주문하고 잔액으로 결제를 수행합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "주문 성공",
            content = @Content(schema = @Schema(implementation = OrderResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomErrorResponse.class),
                examples = @ExampleObject(
                    name = "잘못된 요청 예시",
                    value = """
                    {
                      "message": "상품 수량은 1 이상이어야 합니다.",
                      "status": 400
                    }
                    """
                )
            )
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
    @PostMapping
    ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request);
}
