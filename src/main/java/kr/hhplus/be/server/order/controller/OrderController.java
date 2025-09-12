package kr.hhplus.be.server.order.controller;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.facade.OrderCommandMapper;
import kr.hhplus.be.server.order.facade.OrderFacade;
import kr.hhplus.be.server.order.facade.OrderItemCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderController implements OrderApi {

    private final OrderFacade orderFacade;

    @Override
    public ResponseEntity<OrderResponseDto> createOrder(OrderRequestDto request) {

        List<OrderItemCommand> commands = OrderCommandMapper.toCommandList(request.getItems());

        Order order = orderFacade.placeOrder(request.getUserId(), commands, request.getCouponId());
        OrderResponseDto dto = new OrderResponseDto(order.getId(), order.getTotalAmount());
        return ResponseEntity.status(201).body(dto);
    }
}
