package kr.hhplus.be.server.coupon.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private final StringRedisTemplate redisTemplate;

    public List<Long> getAllPolicyIds() {
        Set<String> keys = redisTemplate.keys("COUPON:POLICY:*:REMAINING");
        return keys.stream()
                .map(key -> key.split(":")[2])
                .map(Long::parseLong)
                .toList();
    }

    public void setRemainingCount(Long policyId, int remainingCount) {
        String remainingKey = CouponRedisKey.remainingKey(policyId);
        redisTemplate.opsForValue().set(remainingKey, String.valueOf(remainingCount));
    }

    public void removePolicy(Long policyId) {
        redisTemplate.delete(CouponRedisKey.remainingKey(policyId));
        redisTemplate.delete(CouponRedisKey.pendingKey(policyId));
        redisTemplate.delete(CouponRedisKey.issuedKey(policyId));
    }

    public List<Long> peekPending(Long policyId, int count) {
        String key = CouponRedisKey.pendingKey(policyId);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().rangeWithScores(key, 0, count - 1);

        if (tuples == null) return List.of();

        return tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    public void removePending(Long policyId, List<Long> userIds) {
        if (userIds.isEmpty()) return;
        String key = CouponRedisKey.pendingKey(policyId);
        redisTemplate.opsForZSet()
                .remove(key, userIds.stream().map(String::valueOf).toArray());
    }

    public boolean tryIssue(Long userId, Long policyId) {
        String remainingKey = CouponRedisKey.remainingKey(policyId);
        String pendingKey = CouponRedisKey.pendingKey(policyId);

        Long remaining = redisTemplate.opsForValue().decrement(remainingKey);
        if (remaining == null || remaining < 0) {
            return false;
        }

        redisTemplate.opsForZSet().add(pendingKey, userId.toString(), getRandScore());

        return true;
    }

    private long getRandScore () {
        int rand = ThreadLocalRandom.current().nextInt(0, 1000);
        String randStr = String.format("%03d", rand); // "001" ~ "999"
        return  Long.parseLong(System.currentTimeMillis() + randStr);
    }

    public void clearAll() {
        redisTemplate.delete(redisTemplate.keys("COUPON:POLICY:*"));
    }
}
