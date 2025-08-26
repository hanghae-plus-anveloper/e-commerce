package kr.hhplus.be.server.analytics.application;

import kr.hhplus.be.server.common.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class TopProductEventHandler {

    private final TopProductService topProductService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(OrderCompletedEvent event) {
        topProductService.recordOrdersAsync(event.orderId(), event.rankingDtoList());
    }
}
