package kr.hhplus.be.server.controller.order;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.balance.BalanceRepository;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponPolicy;
import kr.hhplus.be.server.domain.coupon.CouponPolicyRepository;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
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
                .startedAt(new Date(System.currentTimeMillis() - 1000 * 60))
                .endedAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .build());
        Coupon coupon = new Coupon(
                null,
                policy,
                userId,
                policy.getDiscountAmount(),
                policy.getDiscountRate(),
                false
        );

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
