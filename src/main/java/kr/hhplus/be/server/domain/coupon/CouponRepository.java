package kr.hhplus.be.server.domain.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findAllByUserId(Long userId);
    Optional<Coupon> findByIdAndUserId(Long id, Long userId);
    List<Coupon> findAllByUserIdAndUsedFalse(Long userId);
}
