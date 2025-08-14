package kr.hhplus.be.server.analytics.domain;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TopProductNativeRepository {

    private final EntityManager em;

    public List<TopProductView> findTopSoldBetween(LocalDate from, LocalDate to, int limit) {
        return Collections.emptyList(); // 테스트 컴파일 방지용 함수
    }

}
