package kr.hhplus.be.server.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "USER API", description = "사용자 관련 API 입니다.")
@RequestMapping("/users")
public interface UserApi {

    @Operation(summary = "이름으로 사용자 ID 조회", description = "사용자 이름(u1, u2 등)을 기준으로 userId를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "사용자 없음")
    @GetMapping("/id")
    ResponseEntity<Long> getUserIdByName(@RequestParam String name);
}
