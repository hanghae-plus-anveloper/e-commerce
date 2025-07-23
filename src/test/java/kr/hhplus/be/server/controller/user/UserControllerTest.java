package kr.hhplus.be.server.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.facade.user.UserFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private UserFacade userFacade;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("잔액 조회 요청이 정상 처리된다")
    void getBalance() throws Exception {
        Long userId = 1L;
        int balance = 10000;
        given(userFacade.getBalance(userId))
                .willReturn(new BalanceResponseDto(userId, balance));

        mockMvc.perform(get("/users/{userId}/balance", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.balance").value(balance));
    }

    @Test
    @DisplayName("잔액 충전 요청이 정상 처리된다")
    void chargeBalance() throws Exception {
        Long userId = 1L;
        ChargeRequestDto request = new ChargeRequestDto(1000);
        willDoNothing().given(userFacade).chargeBalance(userId, request.getAmount());

        mockMvc.perform(post("/users/{userId}/balance", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        then(userFacade).should().chargeBalance(userId, request.getAmount());
    }

    @Test
    @DisplayName("음수 금액 입력 시 400 Bad Request가 반환된다")
    void chargeBalance_invalidAmount() throws Exception {
        Long userId = 1L;
        ChargeRequestDto invalidRequest = new ChargeRequestDto(-500);

        mockMvc.perform(post("/users/{userId}/balance", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("충전 금액은 1원 이상이어야 합니다."))
                .andExpect(jsonPath("$.status").value(400));
    }

}
