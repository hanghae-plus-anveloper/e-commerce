package kr.hhplus.be.server.controller.coupon;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import kr.hhplus.be.server.domain.coupon.CouponPolicy;
import kr.hhplus.be.server.domain.coupon.CouponPolicyRepository;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CouponControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private CouponRepository couponRepository;

    private Long userId;
    private Long policyId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        couponRepository.deleteAll();
        userRepository.deleteAll();
        couponPolicyRepository.deleteAll();

        User user = userRepository.save(new User("테스트 유저"));
        userId = user.getId();

        CouponPolicy policy = CouponPolicy.builder()
                .discountAmount(1000)
                .discountRate(0.0)
                .availableCount(10)
                .remainingCount(10)
                .expireDays(30)
                .startedAt(new Date(System.currentTimeMillis() - 1000 * 60)) // 과거 시작
                .endedAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 미래 종료
                .build();
        policyId = couponPolicyRepository.save(policy).getId();
    }

    @Test
    @DisplayName("쿠폰 발급 API - 성공")
    void claimCoupon_success() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("userId", userId)
                .queryParam("policyId", policyId)
                .when()
                .post("/coupons")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("couponId", notNullValue())
                .body("discount", equalTo(1000));
    }

    @Test
    @DisplayName("쿠폰 발급 API - 실패 (정책 없음)")
    void claimCoupon_invalidPolicy() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("userId", userId)
                .queryParam("policyId", 999L)
                .when()
                .post("/coupons")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("존재하지 않는 쿠폰 정책입니다."))
                .body("status", equalTo(400));
    }

    @Test
    @DisplayName("쿠폰 조회 API - 성공")
    void getUserCoupons() {

        given()
                .contentType(ContentType.JSON)
                .queryParam("userId", userId)
                .queryParam("policyId", policyId)
                .when()
                .post("/coupons")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/users/{userId}/coupons", userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("[0].discount", equalTo(1000));
    }
}
