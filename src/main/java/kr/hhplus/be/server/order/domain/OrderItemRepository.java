package kr.hhplus.be.server.order.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("""
        SELECT p.id, p.name, SUM(oi.quantity)
        FROM OrderItem oi
        JOIN oi.product p
        WHERE oi.orderedAt BETWEEN :from AND :to
        GROUP BY p.id, p.name
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<Object[]> findTop5ProductsByOrderItemDate(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );


    @Query(value = """
        EXPLAIN ANALYZE
        SELECT product_id, SUM(quantity)
        FROM order_item
        WHERE ordered_at BETWEEN DATE_SUB(CURDATE(), INTERVAL 3 DAY) AND CURDATE()
        GROUP BY product_id
        ORDER BY SUM(quantity) DESC
        LIMIT 5
    """, nativeQuery = true)
    List<String> explainTop5ByOrderItemOrderedAt();
}