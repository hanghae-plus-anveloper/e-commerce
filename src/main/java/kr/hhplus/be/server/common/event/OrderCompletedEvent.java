package kr.hhplus.be.server.common.event;

import kr.hhplus.be.server.analytics.application.TopProductRankingDto;

import java.util.List;

public record OrderCompletedEvent(Long orderId, Long userId, List<TopProductRankingDto> rankingDtoList) {
}
