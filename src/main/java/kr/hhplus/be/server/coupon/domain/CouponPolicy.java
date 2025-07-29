package kr.hhplus.be.server.coupon.domain;

import java.util.Date;

import jakarta.persistence.*;
import kr.hhplus.be.server.coupon.exception.CouponSoldOutException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "coupon_policy")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    @Temporal(TemporalType.TIMESTAMP)
    private Date startedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date endedAt;

    public boolean isWithinPeriod() {
        Date now = new Date();
        return !now.before(startedAt) && !now.after(endedAt);
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
