package kr.hhplus.be.server.domain.coupon;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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
public class CouponPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int discountAmount; // 정액 할인
    private double discountRate; // 정률 할인

    private int availableCount; // 최초 발급 수량
    private int remainingCount; // 잔여 수량
    private int expireDays; // 발급 기준 사용 가능 일 수

    @Temporal(TemporalType.TIMESTAMP)
    private Date startedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date endedAt;

    public boolean isAvailable() {
        Date now = new Date();
        return remainingCount > 0
                && !now.before(startedAt)
                && !now.after(endedAt);
    }

    public void decreaseRemainingCount() {
        if (remainingCount <= 0) {
            throw new IllegalStateException("남은 쿠폰 수량이 없습니다.");
        }
        this.remainingCount--;
    }
}
