package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.*;
import kr.hhplus.be.server.exception.InvalidCouponException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "COUPON")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private CouponPolicy policy;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private int discountAmount; // 정액 할인
    private double discountRate; // 정률 할인
    private boolean used;

    public void use() {
        if (!isAvailable()) {
            throw new InvalidCouponException("사용할 수 없는 쿠폰입니다.");
        }
        this.used = true;
    }

    public boolean isAvailable() {
        return !used && policy.isWithinPeriod();
    }
}
