package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.IntegrationTestContainersConfig;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import kr.hhplus.be.server.kafka.message.CouponPendedMessage;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(IntegrationTestContainersConfig.class)
public class CouponServiceKafkaTest {

    @Autowired private CouponService couponService;
    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponPolicyRepository couponPolicyRepository;
    @Autowired private UserRepository userRepository;

    @Value("${app.kafka.topics.coupon-pended}")
    private String couponPendedTopic;

    private List<User> users;
    private List<CouponPolicy> policies;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();
        userRepository.deleteAll();
        couponPolicyRepository.deleteAll();

        users = IntStream.rangeClosed(1, 1000)
                .mapToObj(i -> User.builder().name("u-" + i).build())
                .toList();
        userRepository.saveAll(users);

        policies = IntStream.rangeClosed(1, 4)
                .mapToObj(i -> CouponPolicy.builder()
                        .discountAmount(1000)
                        .discountRate(0.0)
                        .availableCount(200)
                        .remainingCount(200)
                        .expireDays(30)
                        .startedAt(LocalDateTime.now().minusDays(1))
                        .endedAt(LocalDateTime.now().plusDays(1))
                        .build())
                .toList();
        couponPolicyRepository.saveAll(policies);

        policies.forEach(p -> couponService.setRemainingCount(p.getId(), 200));

        printHeader("세팅 완료");
        System.out.printf("유저 수: %d, 정책 수: %d, 정책ID: %s%n",
                users.size(), policies.size(),
                policies.stream().map(CouponPolicy::getId).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("4개의 정책에 1000명이 동시 발급 요청을 했을때, 남은 수량만큼만 쿠폰이 발급된다.")
    void issueCoupon_kafka() throws Exception {
        try (KafkaConsumer<String, CouponPendedMessage> monitor = newTestConsumer()) {
            monitor.subscribe(Collections.singletonList(couponPendedTopic));
            monitor.poll(Duration.ofMillis(0));

            int totalRequests = 4_000; // 4정책 × 1,000명
            ExecutorService pool = Executors.newFixedThreadPool(64);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(totalRequests);
            AtomicInteger acceptedCounter = new AtomicInteger();

            printHeader("동시 발급 요청 시작");
            System.out.printf("총 요청 수: %,d (정책 %d개 × 각 1,000명)%n", totalRequests, policies.size());

            for (CouponPolicy policy : policies) {
                for (User u : users) {
                    pool.submit(() -> {
                        try {
                            start.await();
                            boolean ok = couponService.tryIssue(u.getId(), policy.getId());
                            if (ok) acceptedCounter.incrementAndGet();
                        } catch (InterruptedException ignored) {
                        } finally {
                            done.countDown();
                        }
                    });
                }
            }

            start.countDown();
            done.await(10, TimeUnit.SECONDS);
            pool.shutdownNow();

            System.out.printf("요청 완료. Redis 선착순 통과(accepted): %,d%n", acceptedCounter.get());

            printHeader("Kafka 모니터링(테스트 컨슈머) 수집");
            List<ConsumerRecord<String, CouponPendedMessage>> collected = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, CouponPendedMessage> polled = monitor.poll(Duration.ofMillis(500));
                polled.forEach(collected::add);
                if (collected.size() >= 800) break;
            }

            System.out.printf("모니터 수집 메시지 수: %,d (목표 800)%n", collected.size());
            assertThat(collected.size()).isGreaterThanOrEqualTo(800);

            Map<Long, List<ConsumerRecord<String, CouponPendedMessage>>> byPolicy =
                    collected.stream().collect(Collectors.groupingBy(r -> r.value().policyId()));

            for (CouponPolicy p : policies) {
                List<ConsumerRecord<String, CouponPendedMessage>> list =
                        byPolicy.getOrDefault(p.getId(), Collections.emptyList());
                if (list.isEmpty()) continue; // 모니터가 모두 수집 못했을 수 있어 스킵

                long distinctPartitions = list.stream().map(ConsumerRecord::partition).distinct().count();
                assertThat(distinctPartitions)
                        .as("policyId=%d must map to single partition".formatted(p.getId()))
                        .isEqualTo(1);

                long minOffset = Long.MAX_VALUE;
                long maxOffset = Long.MIN_VALUE;
                long last = -1;
                for (ConsumerRecord<String, CouponPendedMessage> r : list) {
                    assertThat(r.offset()).isGreaterThan(last);
                    last = r.offset();
                    minOffset = Math.min(minOffset, r.offset());
                    maxOffset = Math.max(maxOffset, r.offset());
                }
                System.out.printf("policyId=%d\t파티션=%d\t오프셋 범위:\t[%d .. %d]\t%n",
                        p.getId(), list.get(0).partition(), minOffset, maxOffset);
            }

            printHeader("DB 발급 결과 대기");
            awaitTotalIssued(800, 20_000);
            Map<Long, Long> dbByPolicy = countByPolicy();
            printDbSummary(dbByPolicy);

            for (CouponPolicy p : policies) {
                assertThat(dbByPolicy.getOrDefault(p.getId(), 0L))
                        .as("DB issued count per policy")
                        .isEqualTo(200L);
            }
            long total = dbByPolicy.values().stream().mapToLong(Long::longValue).sum();
            assertThat(total).isEqualTo(800L);
        }
    }

    private KafkaConsumer<String, CouponPendedMessage> newTestConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.kafka.bootstrap-servers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "coupon-monitor-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.springframework.kafka.support.serializer.JsonDeserializer");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "kr.hhplus.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "kr.hhplus.be.server.kafka.message.CouponPendedMessage");
        return new KafkaConsumer<>(props);
    }

    private void awaitTotalIssued(long expected, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            long count = couponRepository.count();
            if (count >= expected) return;
            Thread.sleep(200);
        }
        assertThat(couponRepository.count()).isGreaterThanOrEqualTo(expected);
    }

    private Map<Long, Long> countByPolicy() {
        List<Coupon> all = couponRepository.findAll();
        return all.stream().collect(Collectors.groupingBy(c -> c.getPolicy().getId(), Collectors.counting()));
    }

    private void printHeader(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }


    private void printKafkaSummary(Map<Long, List<ConsumerRecord<String, CouponPendedMessage>>> byPolicy) {
        printHeader("Kafka 수집 요약");
        for (Map.Entry<Long, List<ConsumerRecord<String, CouponPendedMessage>>> e : byPolicy.entrySet()) {
            Long policyId = e.getKey();
            List<ConsumerRecord<String, CouponPendedMessage>> list = e.getValue();
            Map<Integer, Long> byPartition = list.stream()
                    .collect(Collectors.groupingBy(ConsumerRecord::partition, Collectors.counting()));
            long total = list.size();
            System.out.printf("policyId=%d 총 수집: %,d, 파티션 분포: %s%n", policyId, total, byPartition);
        }
    }

    private void printDbSummary(Map<Long, Long> dbByPolicy) {
        printHeader("DB 발급 요약");
        long sum = 0;
        for (Map.Entry<Long, Long> e : dbByPolicy.entrySet()) {
            System.out.printf("policyId=%d -> 발급 %,d%n", e.getKey(), e.getValue());
            sum += e.getValue();
        }
        System.out.printf("총 발급: %,d%n", sum);
    }
}
