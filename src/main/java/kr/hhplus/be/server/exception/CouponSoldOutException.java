package kr.hhplus.be.server.exception;

public class CouponSoldOutException extends RuntimeException {
    public CouponSoldOutException(String message) {
        super(message);
    }
}