package kr.hhplus.be.server.controller.balance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.balance.BalanceRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.utility.TestcontainersConfiguration;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class BalanceControllerTest {

    @LocalServerPort
    int port;

    private Long userId;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceRepository balanceRepository;


    @BeforeEach
    void setUp() {
        RestAssured.port = port;

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
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/users/{userId}/balance", userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("userId", equalTo(userId.intValue()))
                .body("balance", equalTo(1000));
    }

    @Test
    @DisplayName("잔액 충전 API - 성공")
    void chargeBalance() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"amount\":500}")
                .when()
                .post("/users/{userId}/balance", userId)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("balance", equalTo(1500));
    }

    @Test
    @DisplayName("음수 충전 API - 실패 (400)")
    void chargeBalance_fail() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"amount\":-100}")
                .when()
                .post("/users/{userId}/balance", userId)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("충전 금액은 1원 이상이어야 합니다."))
                .body("status", equalTo(400));
    }
}
