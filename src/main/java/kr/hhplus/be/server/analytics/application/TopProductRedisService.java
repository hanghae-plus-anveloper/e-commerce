package kr.hhplus.be.server.analytics.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopProductRedisService {

    private final StringRedisTemplate redisTemplate;

    // 당일
    public void recordOrder(String productId, int quantity) {
        recordOrder(productId, quantity, LocalDate.now());
    }

    // 특정 일자
    public void recordOrder(String productId, int quantity, LocalDate date) {
    }

    public List<TopProductRankingDto> getTop5InLast3Days() {
        return List.of();
    }
}
