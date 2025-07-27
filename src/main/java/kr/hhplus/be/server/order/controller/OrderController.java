package kr.hhplus.be.server.order.controller;

import lombok.extern.slf4j.Slf4j;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.facade.OrderFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderController implements OrderApi {

    private final OrderFacade orderFacade;

    @Override
    public ResponseEntity<OrderResponseDto> createOrder(OrderRequestDto request) {
        log.info("주문 요청: userId={}, couponId={}, items={}",
                request.getUserId(), request.getCouponId(), request.getItems());
        Order order = orderFacade.placeOrder(
                request.getUserId(),
                request.getItems(),
                request.getCouponId()
        );
        log.info("주문 생성 완료: orderId={}, totalAmount={}", order.getId(), order.getTotalAmount());

        OrderResponseDto dto = new OrderResponseDto(order.getId(), order.getTotalAmount());
        return ResponseEntity.status(201).body(dto);
    }
}
