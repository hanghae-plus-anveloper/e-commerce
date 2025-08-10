package kr.hhplus.be.server.order.facade;

import kr.hhplus.be.server.order.controller.OrderItemRequestDto;

import java.util.List;
import java.util.stream.Collectors;

public class OrderCommandMapper {

    public static List<OrderItemCommand> toCommandList(List<OrderItemRequestDto> dtos) {
        return dtos.stream()
                .map(dto -> new OrderItemCommand(dto.getProductId(), dto.getQuantity()))
                .collect(Collectors.toList());
    }
}
