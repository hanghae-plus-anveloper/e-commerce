package kr.hhplus.be.server.domain.order;

public enum OrderStatus {
    DRAFT,        // 임시 저장
    PENDING,      // 결제 대기
    PAID,         // 결제 완료
    IN_TRANSIT,   // 배송 중
    CONFIRMED,    // 구매 확정
    CANCELLED,    // 취소됨
    REFUNDED      // 환불됨
}
