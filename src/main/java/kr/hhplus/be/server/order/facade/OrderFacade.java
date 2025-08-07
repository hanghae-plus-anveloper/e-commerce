package kr.hhplus.be.server.order.facade;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.balance.application.BalanceService;
import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.order.application.OrderService;
import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
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
    public Order placeOrder(Long userId, List<OrderItemCommand> orderItems, Long couponId) {
        User user = userService.findById(userId);

        List<OrderItem> items = orderItems.stream()
                .sorted(Comparator.comparing(OrderItemCommand::getProductId)) // 상품 순서 정렬
                .map(command -> {
                    Product product = productService.verifyAndDecreaseStock(command.getProductId(), command.getQuantity());
                    return OrderItem.of(product, product.getPrice(), command.getQuantity(), 0);
                })
                .toList();

        int total = items.stream()
                .mapToInt(OrderItem::getSubtotal)
                .sum();


        if (couponId != null) {
            Coupon coupon = couponService.useCoupon(couponId, user.getId());
            int discount = coupon.getDiscountRate() > 0
                    ? (int) (total * coupon.getDiscountRate())
                    : coupon.getDiscountAmount();
            total = Math.max(0, total - discount);
        }

        balanceService.useBalance(user, total);

        return orderService.createOrder(user, items, total);
    }

}
