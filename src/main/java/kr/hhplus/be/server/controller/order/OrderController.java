package kr.hhplus.be.server.controller.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.exception.*;
import kr.hhplus.be.server.facade.order.OrderFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderFacade orderFacade;

    @Override
    public ResponseEntity<OrderResponseDto> createOrder(OrderRequestDto request) {
        Order order = orderFacade.placeOrder(
                request.getUserId(),
                request.getItems(),
                request.getCouponId()
        );

        OrderResponseDto dto = new OrderResponseDto(order.getId(), order.getTotalAmount());
        return ResponseEntity.status(201).body(dto);
    }
}
