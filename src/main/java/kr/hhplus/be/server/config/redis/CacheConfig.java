package kr.hhplus.be.server.config.redis;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, org.redisson.spring.cache.CacheConfig> configs = new HashMap<>();
        configs.put("default", new org.redisson.spring.cache.CacheConfig(600_000, 0)); // TTL 10분
        return new RedissonSpringCacheManager(redissonClient, configs);
    }
}

