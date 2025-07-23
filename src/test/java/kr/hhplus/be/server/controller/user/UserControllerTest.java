package kr.hhplus.be.server.controller.user;

import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.balance.BalanceRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTest {


    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        balanceRepository.deleteAll();

        User user = new User("테스트유저");
        userId = userRepository.save(user).getId();

        Balance balance = new Balance(user, 1000);
        balanceRepository.save(balance);
    }

    @Test
    @DisplayName("잔액 조회 API - 성공")
    void getBalance() {
        ResponseEntity<BalanceResponseDto> response = restTemplate.getForEntity(
                "/users/" + userId + "/balance",
                BalanceResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo(userId);
        assertThat(response.getBody().getBalance()).isEqualTo(1000);
    }

    @Test
    @DisplayName("잔액 충전 API - 성공")
    void chargeBalance() {
        ChargeRequestDto request = new ChargeRequestDto(500);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChargeRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<BalanceResponseDto> response = restTemplate.postForEntity(
                "/users/" + userId + "/balance",
                entity,
                BalanceResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBalance()).isEqualTo(1500);
    }

    @Test
    @DisplayName("음수 충전 API - 실패 (400)")
    void chargeBalance_fail() {
        ChargeRequestDto request = new ChargeRequestDto(-100);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChargeRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/users/" + userId + "/balance",
                entity,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("충전 금액은 1원 이상이어야 합니다.");
        assertThat(response.getBody().get("status")).isEqualTo(400);
    }

    @DynamicPropertySource
    static void registerPort(DynamicPropertyRegistry registry) {
    }
}
