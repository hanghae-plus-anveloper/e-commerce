package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답 객체")
public class CustomErrorResponse {

    @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다.")
    private String message;

    @Schema(description = "HTTP 상태 코드", example = "404")
    private int status;

    public CustomErrorResponse(String message, int status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }
}
