package kr.hhplus.be.server.coupon.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.coupon.exception.InvalidCouponException;
import kr.hhplus.be.server.user.domain.User;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private CouponPolicy policy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
