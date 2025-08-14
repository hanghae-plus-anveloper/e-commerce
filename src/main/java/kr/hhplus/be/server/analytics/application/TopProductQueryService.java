package kr.hhplus.be.server.analytics.application;

import kr.hhplus.be.server.analytics.domain.TopProductView;
import kr.hhplus.be.server.analytics.domain.TopProductNativeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TopProductQueryService {

    private final TopProductNativeRepository repository;

    public List<TopProductView> top5InLast3Days() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(2);
        return repository.findTopSoldBetween(from, to, 5);
    }
}
