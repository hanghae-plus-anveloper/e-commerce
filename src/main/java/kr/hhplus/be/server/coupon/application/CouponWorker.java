package kr.hhplus.be.server.coupon.application;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponWorker {

    private final CouponService couponService;

    public void processPending(Long id) { // 컴파일만 수행
    }
}
