package kr.hhplus.be.server.coupon.exception;

public class CouponSoldOutException extends RuntimeException {
    public CouponSoldOutException(String message) {
        super(message);
    }
}