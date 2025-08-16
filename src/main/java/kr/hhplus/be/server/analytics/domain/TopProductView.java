package kr.hhplus.be.server.analytics.domain;

public record TopProductView(
        Long productId,
        String name,
        long soldQty
) {}
