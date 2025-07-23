package kr.hhplus.be.server.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.balance.BalanceRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setup() {
        User user = new User("테스트유저");
        savedUser = userRepository.save(user);

        Balance balance = new Balance(savedUser, 1000);
        balanceRepository.save(balance);
    }

    @Test
    @DisplayName("잔액 조회 요청이 정상 처리된다")
    void getBalance() throws Exception {
        mockMvc.perform(get("/users/{userId}/balance", savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(savedUser.getId()))
                .andExpect(jsonPath("$.balance").value(1000));
    }

    @Test
    @DisplayName("잔액 충전 요청이 정상 처리된다")
    void chargeBalance() throws Exception {
        ChargeRequestDto request = new ChargeRequestDto(500);

        mockMvc.perform(post("/users/{userId}/balance", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(savedUser.getId()))
                .andExpect(jsonPath("$.balance").value(1500));
    }

    @Test
    @DisplayName("음수 금액 입력 시 400 Bad Request가 반환된다")
    void chargeBalance_invalidAmount() throws Exception {
        ChargeRequestDto invalidRequest = new ChargeRequestDto(-100);

        mockMvc.perform(post("/users/{userId}/balance", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("충전 금액은 1원 이상이어야 합니다."))
                .andExpect(jsonPath("$.status").value(400));
    }
}
