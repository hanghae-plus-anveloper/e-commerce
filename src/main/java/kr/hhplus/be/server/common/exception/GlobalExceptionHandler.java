package kr.hhplus.be.server.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import kr.hhplus.be.server.balance.exception.InsufficientBalanceException;
import kr.hhplus.be.server.coupon.exception.CouponSoldOutException;
import kr.hhplus.be.server.coupon.exception.InvalidCouponException;
import kr.hhplus.be.server.common.dto.CustomErrorResponse;
import kr.hhplus.be.server.product.exception.ProductNotFoundException;
import kr.hhplus.be.server.user.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 요청 본문 유효성 실패 (ex: @Valid @RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("유효하지 않은 요청입니다.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new CustomErrorResponse(message, HttpStatus.BAD_REQUEST.value()));
    }

    // 쿼리 파라미터 유효성 실패 (ex: @RequestParam, @PathVariable + @Validated)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "message", "잘못된 요청입니다.",
                        "error", ex.getMessage()
                ));
    }

    // 비즈니스 로직: 쿠폰 품절
    @ExceptionHandler(CouponSoldOutException.class)
    public ResponseEntity<CustomErrorResponse> handleCouponSoldOut(CouponSoldOutException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CustomErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value()));
    }

    // 비즈니스 로직: 잔액 부족, 쿠폰 유효성 실패
    @ExceptionHandler({InsufficientBalanceException.class, InvalidCouponException.class})
    public ResponseEntity<CustomErrorResponse> handleBusinessExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CustomErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    // 사용자 없음
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<CustomErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new CustomErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<?> handleProductNotFound(ProductNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", 404,
                        "message", e.getMessage()
                ));
    }


    // 그 외 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleOtherExceptions(HttpServletRequest request, Exception ex) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui")) {
            throw new RuntimeException(ex); // Swagger 요청은 그대로 통과
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "서버 오류가 발생했습니다."));
    }
}
