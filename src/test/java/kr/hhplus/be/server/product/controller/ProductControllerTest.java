package kr.hhplus.be.server.product.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import kr.hhplus.be.server.IntegrationTestContainersConfig;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(IntegrationTestContainersConfig.class)
class ProductControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    private ProductRepository productRepository;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        productRepository.deleteAll();

        product1 = Product.builder().name("상품A").price(1000).stock(10).build();

        product2 = Product.builder().name("상품B").price(2000).stock(5).build();

        product1 = productRepository.save(product1);
        product2 = productRepository.save(product2);
    }

    @Test
    @DisplayName("전체 상품 목록 조회 API - 성공")
    void getAllProducts() {
        given().accept(ContentType.JSON)
                .when().get("/products")
                .then().statusCode(HttpStatus.OK.value())
                .body("size()", is(2))
                .body("[0].name", is("상품A"))
                .body("[1].name", is("상품B"));
    }

    @Test
    @DisplayName("상품 단건 조회 API - 성공")
    void getProductById() {
        given().accept(ContentType.JSON)
                .when().get("/products/{id}", product1.getId())
                .then().statusCode(HttpStatus.OK.value())
                .body("name", is("상품A"))
                .body("price", is(1000))
                .body("stock", is(10));
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 404 반환")
    void getProductById_notFound() {
        given().accept(ContentType.JSON)
                .when().get("/products/{id}", 9999L)
                .then().statusCode(HttpStatus.NOT_FOUND.value());
    }
}
