package kr.hhplus.be.server.common.event.balance;

public record BalanceDeductionFailedEvent(
        Long orderId,
        String reason
) {
}
