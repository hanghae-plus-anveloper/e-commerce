package kr.hhplus.be.server.analytics.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class TopProductRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private static final int TTL_DAYS = 4;
    private static final String PRODUCT_RANKING_PREFIX = "RANKING:PRODUCT:";

    private String getDailyKey(LocalDate date) {
        return PRODUCT_RANKING_PREFIX + date.format(FORMATTER);
    }

    public void recordOrder(String productId, int quantity, LocalDate date) {
        String key = getDailyKey(date);
        redisTemplate.opsForZSet().incrementScore(key, productId, quantity);

        LocalDateTime expireAt = date.plusDays(TTL_DAYS).atStartOfDay();
        Instant instant = expireAt.atZone(ZoneId.systemDefault()).toInstant();
        redisTemplate.expireAt(key, instant);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTop5InLast3Days() {
        LocalDate today = LocalDate.now();
        List<String> keys = List.of(
                getDailyKey(today),
                getDailyKey(today.minusDays(1)),
                getDailyKey(today.minusDays(2))
        );

        String unionKey =  PRODUCT_RANKING_PREFIX + "TOP5LAST3DAYS";

        // 합집합 새로 저장
        redisTemplate.opsForZSet().unionAndStore(keys.get(0), keys.subList(1, keys.size()), unionKey);

        // score 값을 포함한 튜플 조회
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(unionKey, 0, 4);

        return tuples != null ? tuples : Set.of();
    }
}
