package kr.hhplus.be.server.coupon.facade;

import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CouponFacadeTest {

    @Mock
    private UserService userService;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponFacade couponFacade;

    private User user;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = User.builder().name("테스트 유저").build();
        user.setId(1L);

        coupon = Coupon.builder()
                .id(100L)
                .user(user)
                .discountAmount(1000)
                .used(false)
                .build();
    }

    @Test
    @DisplayName("쿠폰 발급 - 성공")
    void issueCoupon_success() {
        Long userId = 1L;
        Long policyId = 10L;

        when(userService.findById(userId)).thenReturn(user);
        when(couponService.issueCoupon(user.getId(), policyId)).thenReturn(coupon);

        Coupon result = couponFacade.issueCoupon(userId, policyId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUser().getId()).isEqualTo(userId);

        verify(userService).findById(userId);
        verify(couponService).issueCoupon(user.getId(), policyId);
    }

    @Test
    @DisplayName("사용자의 쿠폰 목록 조회 - 성공")
    void getCoupons_success() {
        Long userId = 1L;
        List<Coupon> expected = List.of(coupon);

        when(couponService.getCoupons(userId)).thenReturn(expected);

        List<Coupon> result = couponFacade.getCoupons(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(coupon.getId());

        verify(couponService).getCoupons(userId);
    }
}
