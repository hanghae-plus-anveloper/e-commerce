package kr.hhplus.be.server.api;

import io.swagger.v3.oas.annotations.media.ExampleObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.dto.BalanceResponseDto;
import kr.hhplus.be.server.dto.ChargeRequestDto;
import kr.hhplus.be.server.dto.CustomErrorResponse;

@Tag(name = "USER API", description = "사용자 관련 API 입니다.")
@RequestMapping("/users")
public interface UserApi {

    @Operation(summary = "잔액 조회", description = "사용자의 잔액을 조회합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BalanceResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "사용자 ID를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomErrorResponse.class)
            )
        )
    })
    @GetMapping("/{userId}/balance")
    ResponseEntity<BalanceResponseDto> getBalance(@PathVariable Long userId);

    @Operation(summary = "잔액 충전", description = "사용자의 잔액을 충전합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "충전 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BalanceResponseDto.class))
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
                      "message": "충전 금액은 1 이상이어야 합니다.",
                      "status": 400
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "사용자 ID를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomErrorResponse.class)
            )
        )
    })
    @PostMapping("/{userId}/balance")
    ResponseEntity<BalanceResponseDto> chargeBalance(
        @PathVariable Long userId,
        @Valid @RequestBody ChargeRequestDto request
    );
}
