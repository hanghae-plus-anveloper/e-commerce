package kr.hhplus.be.server.coupon.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE CouponPolicy cp
           SET cp.remainingCount = cp.remainingCount - 1
         WHERE cp.id = :policyId
           AND cp.remainingCount > 0
    """)
    int decreaseRemainingCount(@Param("policyId") Long policyId);
}
