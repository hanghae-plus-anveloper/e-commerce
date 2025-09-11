package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.common.lock.LockKey;
import kr.hhplus.be.server.order.facade.OrderItemCommand;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import kr.hhplus.be.server.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long productId) {
        return productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));
    }

    @Transactional
    public Product verifyAndDecreaseStock(Long productId, int requiredQuantity) {
//        Product product = productRepository.findByIdForUpdate(productId)
//                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));
//
//        if (product.getStock() < requiredQuantity) {
//            throw new IllegalStateException("상품 재고가 부족합니다.");
//        }
//
//        product.decreaseStock(requiredQuantity);
//        return product;

        int updatedRows = productRepository.decreaseStockIfAvailable(productId, requiredQuantity);

        if (updatedRows == 0) {
            throw new IllegalStateException("상품 재고가 부족하거나 상품을 찾을 수 없습니다.");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));
    }

    @Transactional
    @DistributedLock(prefix = LockKey.PRODUCT, ids = "#orderItems.![productId]")
    public List<ProductReservation> reserveProducts(List<OrderItemCommand> orderItems) {
        return orderItems.stream()
                .sorted(Comparator.comparing(OrderItemCommand::getProductId))
                .map(command -> {
                    Product product = verifyAndDecreaseStock(command.getProductId(), command.getQuantity());
                    return new ProductReservation(product, product.getPrice(), command.getQuantity());
                })
                .toList();
    }
}
