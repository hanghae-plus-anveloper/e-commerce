package kr.hhplus.be.server.coupon.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.coupon.exception.CouponSoldOutException;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "coupon_policy", indexes = {
        @Index(name = "idx_coupon_policy_period", columnList = "started_at, ended_at")
})
public class CouponPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int discountAmount;     // 정액 할인
    private double discountRate;    // 정률 할인

    private int availableCount;     // 최초 발급 수량
    private int remainingCount;     // 잔여 수량
    private int expireDays;         // 발급 기준 사용 가능 일 수

    @Version
    private int version;            // 낙관적 락 추가

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    public boolean isWithinPeriod() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startedAt) && !now.isAfter(endedAt);
    }

    public boolean hasRemainingCount() {
        return remainingCount > 0;
    }

    public void decreaseRemainingCount() {
        if (remainingCount <= 0) {
            throw new CouponSoldOutException("남은 쿠폰 수량이 없습니다.");
        }
        this.remainingCount--;
    }
}
