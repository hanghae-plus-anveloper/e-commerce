package kr.hhplus.be.server.balance.domain;

import kr.hhplus.be.server.IntegrationTestContainersConfig;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Import(IntegrationTestContainersConfig.class)
class BalanceRepositoryIntegrationTest {

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setup() {
        user = userRepository.save(User.builder().name("테스터").build());
    }

    // lazy loading 으로 연결된 balance.histories 접근 시 세션 만료 에러,
    // >> transactional로 테스트 간 세션 유지
    @Transactional
    @Test
    @DisplayName("잔액을 충전하면 저장된 값으로 조회된다")
    void chargeAndFindBalance() {
        Balance balance = Balance.builder()
                .user(user)
                .balance(0)
                .build();
        balanceRepository.save(balance);

        balance.charge(10_000);
        balanceRepository.save(balance);

        Balance saved = balanceRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(saved.getBalance()).isEqualTo(10_000);
        assertThat(saved.getHistories()).hasSize(1);
    }

    @Transactional
    @Test
    @DisplayName("잔액을 사용하면 차감되고 히스토리가 추가된다")
    void useBalanceSuccessfully() {
        Balance balance = Balance.builder()
                .user(user)
                .balance(20_000)
                .build();
        balanceRepository.save(balance);

        balance.use(5_000);
        balanceRepository.save(balance);

        Balance saved = balanceRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(saved.getBalance()).isEqualTo(15_000);
        assertThat(saved.getHistories()).hasSize(1);
        assertThat(saved.getHistories().get(0).getAmount()).isEqualTo(-5_000);
    }

    @Transactional
    @Test
    @DisplayName("잔액이 부족하면 사용 시도 시 예외가 발생한다")
    void useBalanceWithInsufficientAmount() {
        Balance balance = Balance.builder()
                .user(user)
                .balance(2_000)
                .build();
        balanceRepository.save(balance);

        assertThatThrownBy(() -> balance.use(5_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액 부족");

        // 저장하지 않았으므로 histories도 비어 있어야 함
        Balance saved = balanceRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(saved.getBalance()).isEqualTo(2_000);
        assertThat(saved.getHistories()).isEmpty();
    }

}
