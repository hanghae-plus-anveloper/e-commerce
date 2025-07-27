package kr.hhplus.be.server.product.application;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.product.controller.ProductResponseDto;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import kr.hhplus.be.server.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponseDto::from)
                .toList();
    }

    public ProductResponseDto getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));
        return ProductResponseDto.from(product);
    }

    @Transactional
    public int verifyDecreaseStock(Long productId, int requiredQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));
        if (product.getStock() < requiredQuantity) {
            throw new IllegalStateException("상품 재고가 부족합니다.");
        }
        product.decreaseStock(requiredQuantity);
        return product.getPrice();
    }
}
