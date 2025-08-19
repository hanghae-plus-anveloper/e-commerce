package kr.hhplus.be.server.analytics.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopProductRedisService {

    private final StringRedisTemplate redisTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private String getDailyKey(LocalDate date) {
        return "RANKING:PRODUCT:" + date.format(FORMATTER);
    }

    // 당일
    public void recordOrder(String productId, int quantity) {
        recordOrder(productId, quantity, LocalDate.now());
    }

    // 특정 일자
    public void recordOrder(String productId, int quantity, LocalDate date) {
        String key = getDailyKey(date);
        redisTemplate.opsForZSet().incrementScore(key, productId, quantity);
    }

    public List<TopProductRankingDto> getTop5InLast3Days() {
        LocalDate today = LocalDate.now();
        List<String> keys = List.of(
                getDailyKey(today),
                getDailyKey(today.minusDays(1)),
                getDailyKey(today.minusDays(2))
        );

        String unionKey = "RANKING:PRODUCT:TOP5LAST3DAYS";

        // 합집합 새로 저장
        redisTemplate.opsForZSet().unionAndStore(keys.get(0), keys.subList(1, keys.size()), unionKey);

        // score 값을 포함한 튜플 조회
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(unionKey, 0, 4);

        if (tuples == null) {
            return List.of();
        }

        return tuples.stream()
                .map(t -> new TopProductRankingDto(
                        t.getValue(),
                        t.getScore() != null ? t.getScore().intValue() : 0
                ))
                .collect(Collectors.toList());
    }
}
