package kr.hhplus.be.server.product.controller;

import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController implements ProductApi {

    private final ProductService productService;

    @Override
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        List<Product> products = productService.findAll();
        List<ProductResponseDto> response = products.stream()
                .map(ProductResponseDto::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ProductResponseDto> getProductById(Long productId) {
        Product product = productService.findById(productId);
        return ResponseEntity.ok(ProductResponseDto.from(product));
    }
}
