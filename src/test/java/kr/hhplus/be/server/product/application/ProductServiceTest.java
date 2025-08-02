package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import kr.hhplus.be.server.product.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private ProductRepository productRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productService = new ProductService(productRepository);
    }

    @Test
    @DisplayName("전체 상품 목록을 반환한다")
    void testFindAll() {
        List<Product> products = List.of(
                new Product("USB-C 충전기", 19900, 25),
                new Product("노트북 거치대", 39900, 10)
        );
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("USB-C 충전기");
        assertThat(result.get(0).getStock()).isEqualTo(25);
    }

    @Test
    @DisplayName("ID로 단일 상품을 조회한다")
    void testFindById() {
        Product product = new Product("USB-C 충전기", 19900, 25);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.findById(1L);

        assertThat(result.getName()).isEqualTo("USB-C 충전기");
        assertThat(result.getStock()).isEqualTo(25);
    }

    @Test
    @DisplayName("존재하지 않는 ID 조회 시 예외를 던진다")
    void testFindById_NotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("상품을 찾을 수 없습니다.");
    }
}
