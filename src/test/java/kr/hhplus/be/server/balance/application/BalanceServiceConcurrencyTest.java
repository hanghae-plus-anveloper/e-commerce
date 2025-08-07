package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.*;
        import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BalanceServiceConcurrencyTest {

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        balanceRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .name("test-user")
                .build());

        balanceService.chargeBalance(user, 5000);
    }

    @Test
    @DisplayName("동시에 두 번의 주문 요청이 들어올 경우 잔액이 음수가 되지 않아야 한다")
    void useBalance_concurrently() throws InterruptedException {
        int threadCount = 2;
        int amountToUse = 3000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        ConcurrentMap<String, AtomicInteger> exceptionMap = new ConcurrentHashMap<>();

        for (int i = 0; i < threadCount; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    balanceService.useBalance(user, amountToUse);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptionMap
                            .computeIfAbsent(e.getClass().getSimpleName(), key -> new AtomicInteger(0))
                            .incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, executor);
        }

        latch.await();
        executor.shutdown();

        Balance result = balanceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("잔액 정보 없음"));

        System.out.println("최종 잔액: " + result.getBalance());
        System.out.println("성공 요청 수: " + successCount.get());
        System.out.println("실패 예외 현황:");
        for (Map.Entry<String, AtomicInteger> entry : exceptionMap.entrySet()) {
            System.out.println(" - " + entry.getKey() + ": " + entry.getValue().get());
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(result.getBalance()).isEqualTo(2000);
    }

    @Test
    @DisplayName("충전과 사용이 동시에 발생할 때 잔액이 손실되지 않아야 한다")
    void chargeAndUse_concurrently() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        ConcurrentMap<String, AtomicInteger> exceptionMap = new ConcurrentHashMap<>();

        CompletableFuture.runAsync(() -> {
            try {
                balanceService.chargeBalance(user, 3000);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionMap
                        .computeIfAbsent(e.getClass().getSimpleName(), key -> new AtomicInteger(0))
                        .incrementAndGet();
            } finally {
                latch.countDown();
            }
        }, executor);

        CompletableFuture.runAsync(() -> {
            try {
                balanceService.useBalance(user, 3000);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionMap
                        .computeIfAbsent(e.getClass().getSimpleName(), key -> new AtomicInteger(0))
                        .incrementAndGet();
            } finally {
                latch.countDown();
            }
        }, executor);

        latch.await();
        executor.shutdown();

        Balance result = balanceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("잔액 정보 없음"));
        int actual = result.getBalance();

        System.out.println("최종 잔액: " + actual);
        System.out.println("성공 요청 수: " + successCount.get());
        System.out.println("실패 예외 현황:");
        for (Map.Entry<String, AtomicInteger> entry : exceptionMap.entrySet()) {
            System.out.println(" - " + entry.getKey() + ": " + entry.getValue().get());
        }

        assertThat(actual).isIn(2000, 5000, 8000); // 어떤게 실패할지 모름
        assertThat(successCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("충전과 사용이 완전히 동시에 발생할 때 무결성 검증")
    void chargeAndUse_simultaneously() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        ConcurrentMap<String, AtomicInteger> exceptionMap = new ConcurrentHashMap<>();

        Runnable chargeTask = () -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                balanceService.chargeBalance(user, 3000);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionMap
                        .computeIfAbsent(e.getClass().getSimpleName(), key -> new AtomicInteger(0))
                        .incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable useTask = () -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                balanceService.useBalance(user, 3000);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionMap
                        .computeIfAbsent(e.getClass().getSimpleName(), key -> new AtomicInteger(0))
                        .incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(chargeTask);
        executor.submit(useTask);

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        Balance result = balanceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("잔액 정보 없음"));
        int actual = result.getBalance();

        System.out.println("최종 잔액: " + actual);
        System.out.println("성공 요청 수: " + successCount.get());
        System.out.println("실패 예외 현황:");
        for (Map.Entry<String, AtomicInteger> entry : exceptionMap.entrySet()) {
            System.out.println(" - " + entry.getKey() + ": " + entry.getValue().get());
        }

        assertThat(actual).isIn(2000, 5000, 8000);
        assertThat(successCount.get()).isBetween(1, 2);
    }
}
