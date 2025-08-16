package kr.hhplus.be.server.analytics.application;

import kr.hhplus.be.server.analytics.domain.TopProductNativeRepository;
import kr.hhplus.be.server.analytics.domain.TopProductView;
import kr.hhplus.be.server.common.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@CacheConfig(cacheNames = CacheNames.TOP_PRODUCTS)
public class TopProductQueryService {

    private final TopProductNativeRepository repository;


    @Cacheable(
        key = "T(kr.hhplus.be.server.common.cache.CacheKey).TOP_PRODUCTS"
                + ".key('LAST_N_DAYS', 3, 'TOP', 5)",
        sync = true
    )
    public List<TopProductView> top5InLast3Days() {
        return topNLastNDays(3, 5);
    }

    @Cacheable(
        key = "T(kr.hhplus.be.server.common.cache.CacheKey).TOP_PRODUCTS"
                + ".key('LAST_N_DAYS', #days, 'TOP', #limit)",
        sync = true
    )
    public List<TopProductView> topNLastNDays(int days, int limit) {
        if (days <= 0) throw new IllegalArgumentException("days 는 1 이상이어야 합니다.");
        if (limit <= 0) throw new IllegalArgumentException("limit 는 1 이상이어야 합니다.");

        LocalDate today = LocalDate.now(); // 오늘을 제외하고,
        LocalDate from = today.minusDays(days);
        LocalDate to = today.minusDays(1);
        return repository.findTopSoldBetween(from, to, limit);
    }
}
