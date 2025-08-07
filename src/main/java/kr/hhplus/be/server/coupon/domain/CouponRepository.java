package kr.hhplus.be.server.coupon.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findAllByUserId(Long userId);
    Optional<Coupon> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT c FROM Coupon c JOIN FETCH c.user")
    List<Coupon> findAllWithUser();


    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Coupon c
           SET c.used = true
         WHERE c.id = :couponId
           AND c.user.id = :userId
           AND c.used = false
    """)
    int markCouponAsUsed(@Param("couponId") Long couponId, @Param("userId") Long userId);
}
