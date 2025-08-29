package kr.hhplus.be.server.analytics.application;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import kr.hhplus.be.server.common.event.order.OrderCompletedEvent;
import kr.hhplus.be.server.common.event.order.OrderLineSummary;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TopProductEventHandler {

    private final TopProductService topProductService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(OrderCompletedEvent event) {
        List<TopProductRankingDto> dtos = event.lines().stream()
                .map(this::toDto)
                .toList();
        topProductService.recordOrdersAsync(event.orderId(), dtos);
    }

    private TopProductRankingDto toDto(OrderLineSummary line) {
        String pid = (line.productId() == null) ? null : line.productId().toString();
        return new TopProductRankingDto(pid, line.quantity());
    }
}
