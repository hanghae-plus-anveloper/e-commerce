package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.event.order.OrderRequestedEvent;
import kr.hhplus.be.server.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderRepository orderRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(OrderRequestedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.markPending();
            orderRepository.save(order);
            log.info("[ORDER] order={} moved to PENDING", event.orderId());
        });
    }
}
