package kr.hhplus.be.server.order.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        SELECT p.id, p.name, SUM(oi.quantity)
        FROM Order o
        JOIN o.items oi
        JOIN oi.product p
        WHERE o.orderedAt BETWEEN :from AND :to
        GROUP BY p.id, p.name
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<Object[]> findTop5ProductsByOrderDate(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
