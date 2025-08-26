package kr.hhplus.be.server.analytics.application;

import kr.hhplus.be.server.analytics.infrastructure.TopProductRecord;

public class TopProductMapper {
    public static TopProductRecord toRecord(TopProductRankingDto dto) {
        return new TopProductRecord(dto.productId(), dto.soldQty());
    }
}
