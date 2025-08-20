package kr.hhplus.be.server.coupon.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponRedisService {

    public void clearAll() { // 초기화 임시 메서드
    }

    public boolean tryIssue(Long id, Long id1) {
        return false;
    }
}
