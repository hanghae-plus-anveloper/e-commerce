package kr.hhplus.be.server.facade.order;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.application.balance.BalanceService;
import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.application.order.OrderService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.application.user.UserService;
import kr.hhplus.be.server.controller.order.OrderItemRequestDto;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final UserService userService;
    private final ProductService productService;
    private final CouponService couponService;
    private final OrderService orderService;
    private final BalanceService balanceService;

    @Transactional
    public Order placeOrder(Long userId, List<OrderItemRequestDto> itemDtos, Long couponId) {
        User user = userService.findById(userId);

        List<OrderItem> items = itemDtos.stream()
                .map(dto -> {
                    productService.verifyStock(dto.getProductId(), dto.getQuantity());
                    int price = productService.getPrice(dto.getProductId());
                    return OrderItem.of(dto.getProductId(), price, dto.getQuantity(), 0);
                })
                .toList();

        int total = items.stream()
                .mapToInt(OrderItem::getSubtotal)
                .sum();

        Coupon coupon = null;
        if (couponId != null) {
            coupon = couponService.findValidCouponOrThrow(couponId, user.getId());
            int discount = coupon.getDiscountRate() > 0
                    ? (int) (total * coupon.getDiscountRate())
                    : coupon.getDiscountAmount();
            total = Math.max(0, total - discount);
        }

        balanceService.useBalance(user, total);

        return orderService.createOrder(user.getId(), items, total);
    }

}
