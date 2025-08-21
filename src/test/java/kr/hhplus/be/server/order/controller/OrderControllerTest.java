package kr.hhplus.be.server.order.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import kr.hhplus.be.server.IntegrationTestContainersConfig;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import kr.hhplus.be.server.order.domain.OrderRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(IntegrationTestContainersConfig.class)
class OrderControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long userId;
    private Long productId;
    private Long couponId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        orderRepository.deleteAll();
        couponRepository.deleteAll();
        balanceRepository.deleteAll();
        productRepository.deleteAll();
        couponPolicyRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(new User("테스트 유저"));
        userId = user.getId();
        balanceRepository.save(new Balance(user, 100_000));

        Product product = productRepository.save(new Product("테스트 상품", 20_000, 10));
        productId = product.getId();

        CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.builder()
                .discountAmount(5_000)
                .availableCount(10)
                .remainingCount(10)
                .expireDays(7)
                .startedAt(LocalDateTime.now().minusMinutes(1))
                .endedAt(LocalDateTime.now().plusHours(1))
                .build());

        Coupon coupon = Coupon.builder()
                .userId(userId)
                .user(user)
                .policy(policy)
                .discountAmount(policy.getDiscountAmount())
                .discountRate(policy.getDiscountRate())
                .used(false)
                .build();

        couponId = couponRepository.save(coupon).getId();
    }

    @Test
    @DisplayName("주문 생성 API - 성공")
    void createOrder_success() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "userId": %d,
                    "couponId": %d,
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 2
                        }
                    ]
                }
                """.formatted(userId, couponId, productId))
                .when()
                .post("/orders")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("orderId", notNullValue())
                .body("totalAmount", equalTo(35_000));
    }
}
