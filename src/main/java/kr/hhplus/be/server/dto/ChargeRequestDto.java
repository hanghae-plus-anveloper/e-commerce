package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "잔액 충전 요청 DTO")
public class ChargeRequestDto {
    @Schema(description = "충전 금액", example = "10000")
    @Min(value = 1, message = "충전 금액은 1원 이상이어야 합니다.")
    private int amount;
}