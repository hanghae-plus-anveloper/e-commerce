package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.api.ProductApi;
import kr.hhplus.be.server.dto.ProductResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController implements ProductApi {

    @Override
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        List<ProductResponseDto> mockProducts = List.of(
            new ProductResponseDto(1L, "USB-C 충전기", 19900, 25),
            new ProductResponseDto(2L, "노트북 거치대", 39900, 10)
        );
        return ResponseEntity.ok(mockProducts);
    }

    @Override
    public ResponseEntity<ProductResponseDto> getProductById(Long productId) {
        if (productId == 1L) {
            ProductResponseDto product = new ProductResponseDto(1L, "USB-C 충전기", 19900, 25);
            return ResponseEntity.ok(product);
        } else if (productId == 2L) {
            ProductResponseDto product = new ProductResponseDto(2L, "노트북 거치대", 39900, 10);
            return ResponseEntity.ok(product);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
