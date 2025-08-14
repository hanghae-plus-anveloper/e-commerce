package kr.hhplus.be.server.config.redis;

import kr.hhplus.be.server.common.cache.CacheNames;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class RadissonCacheConfig {

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, CacheConfig> configs = new HashMap<>();
        configs.put(CacheNames.DEFAULT, new CacheConfig(600_000, 0)); // TTL 10분


        long ttlSeconds = secondsUntilMidnightWithJitter(0, 300); // 자정까지 + 0~300초 지터
        long maxIdleSeconds = 0;

        CacheConfig topProductsCfg = new CacheConfig(
                Duration.ofSeconds(ttlSeconds).toMillis(),
                Duration.ofSeconds(maxIdleSeconds).toMillis()
        );
        configs.put(CacheNames.TOP_PRODUCTS, topProductsCfg);

        return new RedissonSpringCacheManager(redissonClient, configs);
    }


    // 자정 까지 남은 초 + 지터
    private long secondsUntilMidnightWithJitter(int minJitterSec, int maxJitterSec) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atStartOfDay();
        long base = java.time.Duration.between(now, midnight).getSeconds();
        int jitter = (maxJitterSec > minJitterSec)
                ? ThreadLocalRandom.current().nextInt(minJitterSec, maxJitterSec + 1)
                : 0;
        return Math.max(1, base + jitter);
    }
}

