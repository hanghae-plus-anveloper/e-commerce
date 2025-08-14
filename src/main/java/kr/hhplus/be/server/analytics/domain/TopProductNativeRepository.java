// src/main/java/kr/hhplus/be/server/analytics/domain/TopProductNativeRepository.java
package kr.hhplus.be.server.analytics.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TopProductNativeRepository {

    @PersistenceContext
    private final EntityManager em;

    public List<TopProductView> findTopSoldBetween(LocalDate from, LocalDate to, int limit) {
        String sql = """
            SELECT p.id AS product_id,
                   p.name AS name,
                   SUM(oi.quantity) AS sold_qty
              FROM order_item oi
              JOIN product p ON p.id = oi.product_id
             WHERE oi.ordered_date BETWEEN :from AND :to
             GROUP BY p.id, p.name
             ORDER BY sold_qty DESC, p.id ASC
            """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("from", Date.valueOf(from));
        q.setParameter("to",   Date.valueOf(to));
        q.setMaxResults(limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        return rows.stream()
                .map(r -> new TopProductView(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        ((Number) r[2]).longValue()
                ))
                .toList();
    }
}
