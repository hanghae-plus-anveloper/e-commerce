package kr.hhplus.be.server.product.application;


import kr.hhplus.be.server.common.event.product.StockReserveFailedEvent;
import kr.hhplus.be.server.common.event.product.StockReservedEvent;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import kr.hhplus.be.server.saga.domain.OrderSagaItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public void reserveStock(Long orderId, List<OrderSagaItem> items) {
        try {
            int subTotal = 0;

            for (OrderSagaItem item : items) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + item.getProductId()));

                if (product.getStock() < item.getQuantity()) {
                    throw new IllegalStateException("재고 부족: productId=" + product.getId());
                }

                product.decreaseStock(item.getQuantity());
                subTotal += product.getPrice() * item.getQuantity();
            }

            log.info("[PRODUCT] order={} stock reserved successfully, subTotal={}", orderId, subTotal);

            // 성공 이벤트 발행
            publisher.publishEvent(new StockReservedEvent(orderId, subTotal));

        } catch (Exception e) {
            log.warn("[PRODUCT] order={} stock reservation failed, reason={}", orderId, e.getMessage());
            publisher.publishEvent(new StockReserveFailedEvent(orderId, e.getMessage()));
        }
    }

    @Transactional
    public void restoreStock(Long orderId, List<OrderSagaItem> items) {
        for (OrderSagaItem item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 없음: " + item.getProductId()));
            product.increaseStock(item.getQuantity());
        }
    }
}
