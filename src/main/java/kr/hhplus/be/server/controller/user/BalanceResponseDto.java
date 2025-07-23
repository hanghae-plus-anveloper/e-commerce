package kr.hhplus.be.server.controller.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "잔액 응답 DTO")
public class BalanceResponseDto {
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "현재 잔액", example = "2500")
    private int balance;
}
