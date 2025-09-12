package kr.hhplus.be.server.product.application;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.common.lock.LockKey;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import kr.hhplus.be.server.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));
    }

    @Transactional
    public Product verifyAndDecreaseStock(Long productId, int requiredQuantity) {
        int updatedRows = productRepository.decreaseStockIfAvailable(productId, requiredQuantity);

        if (updatedRows == 0) {
            throw new IllegalStateException("상품 재고가 부족하거나 상품을 찾을 수 없습니다.");
        }

        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));
    }

    @Transactional
    @DistributedLock(prefix = LockKey.PRODUCT, ids = "#requests.![productId]")
    public List<ProductReservation> reserveProducts(List<ProductReservationRequest> requests) {
        return requests.stream()
                .sorted(Comparator.comparing(ProductReservationRequest::productId))
                .map(request -> {
                    Product product = verifyAndDecreaseStock(request.productId(), request.quantity());
                    return new ProductReservation(product, product.getPrice(), request.quantity());
                })
                .toList();
    }
}
