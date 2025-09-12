package kr.hhplus.be.server.order.facade;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.application.BalanceService;
import kr.hhplus.be.server.common.event.order.OrderCompletedEvent;
import kr.hhplus.be.server.common.event.order.OrderLineSummary;
import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.order.application.OrderService;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.application.ProductReservationRequest;
import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.user.domain.User;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;
    private final ProductService productService;
    private final CouponService couponService;
    private final OrderService orderService;
    private final BalanceService balanceService;
    // private final TopProductService topProductService;

    @Transactional
    // @DistributedLock(prefix = LockKey.PRODUCT, ids = "#orderItems.![productId]")
    public Order placeOrder(Long userId, List<OrderItemCommand> orderItems, Long couponId) {
        User user = userService.findById(userId);

        List<ProductReservationRequest> requests = orderItems.stream()
                .map(i -> new ProductReservationRequest(i.getProductId(), i.getQuantity()))
                .toList();

        List<OrderItem> items = productService.reserveProducts(requests).stream()
                .map(r -> OrderItem.of(r.getProduct(), r.getPrice(), r.getQuantity(), 0))
                .toList();

        int total = items.stream()
                .mapToInt(OrderItem::getSubtotal)
                .sum();

        int discount = 0;
        if (couponId != null) {
            Coupon coupon = couponService.useCoupon(couponId, user.getId());
            discount = coupon.getDiscountRate() > 0
                    ? (int) (total * coupon.getDiscountRate())
                    : coupon.getDiscountAmount();
        }

        balanceService.useBalance(user, total);

        Order order = orderService.createOrder(user, items, total, couponId, discount);

        List<OrderLineSummary> lines = items.stream()
                .map(i -> new OrderLineSummary(i.getProduct().getId(), i.getQuantity()))
                .toList();

        eventPublisher.publishEvent(new OrderCompletedEvent(order.getId(), userId, lines));

        return order;
    }

}
