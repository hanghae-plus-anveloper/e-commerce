package kr.hhplus.be.server.coupon.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findAllByUserId(Long userId);
    Optional<Coupon> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT c FROM Coupon c JOIN FETCH c.user")
    List<Coupon> findAllWithUser();
}
