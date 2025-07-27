package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public Order createOrder(Long userId, List<OrderItem> items, int totalAmount) {
        Order order = Order.create(userId, items, totalAmount);
        return orderRepository.save(order);
    }
}
