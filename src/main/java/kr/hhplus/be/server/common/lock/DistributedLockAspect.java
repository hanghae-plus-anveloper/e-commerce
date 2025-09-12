package kr.hhplus.be.server.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE) // 우선순위 @Tran
public class DistributedLockAspect {
    private static final String LOCK_PREFIX = "LOCK:";

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(kr.hhplus.be.server.common.lock.DistributedLock)") // 포인트 컷
    public Object lock(ProceedingJoinPoint joinPoint)  throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        StandardEvaluationContext ctx = new StandardEvaluationContext();

        String[] paramNames = sig.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }

        Expression exp = parser.parseExpression(distributedLock.ids()); // ids SpEL 평가
        Object idsVal = exp.getValue(ctx);
        String prefixStr = distributedLock.prefix().name();

        List<String> lockKeys = toKeys(prefixStr, idsVal).stream().sorted().toList();; // 키 목록 생성, 정렬

        if (lockKeys.isEmpty()) {
            return joinPoint.proceed();
        }

        List<RLock> lockList = lockKeys.stream()
                .map(redissonClient::getLock)
                .toList();
        RedissonMultiLock multiLock = new RedissonMultiLock(lockList.toArray(new RLock[0]));

        try {
            boolean locked = multiLock.tryLock(
                    distributedLock.waitTimeoutMillis(),
                    distributedLock.ttlMillis(),
                    TimeUnit.MILLISECONDS
            );
            if (!locked) {
                throw new IllegalStateException("Failed to acquire multi lock: " + lockKeys);
            }
            return joinPoint.proceed();
        } finally {
            try {
                multiLock.unlock(); // 하나라도 잡혀 있으면 모두 해제
            } catch (IllegalMonitorStateException e) {
                log.info("MultiLock already unlocked: keys={}", lockKeys);
            }
        }

//        List<RLock> acquiredLocks = new ArrayList<>();
//
//        try {
//            for (String key : lockKeys) {
//                RLock lock = redissonClient.getLock(key); // 정렬된 순서대로 락획득(수동)
//                boolean locked = lock.tryLock(
//                        distributedLock.waitTimeoutMillis(),
//                        distributedLock.ttlMillis(),
//                        TimeUnit.MILLISECONDS
//                );
//                if (!locked) {
//                    for (RLock l : acquiredLocks) {
//                        try { l.unlock(); } catch (Exception ignore) {}
//                    }
//                    throw new IllegalStateException("Failed to acquire lock: " + key);
//                }
//                acquiredLocks.add(lock);
//            }
//
//            return joinPoint.proceed(); // 비즈니스 로직 실행
//        } finally {
//            Collections.reverse(acquiredLocks);
//            for (RLock l : acquiredLocks) {
//                try { l.unlock(); } catch (IllegalMonitorStateException e) {
//                    log.info("Lock already unlocked: {}", l.getName());
//                }
//            }
//        }
    }

    @SuppressWarnings("unchecked")
    private List<String> toKeys(String prefix, Object idsVal) {
        Iterable<Object> iterable;
        if (idsVal instanceof Iterable<?> it) {
            iterable = (Iterable<Object>) it;
        } else if (idsVal != null && idsVal.getClass().isArray()) {
            List<Object> list = new ArrayList<>();
            int len = java.lang.reflect.Array.getLength(idsVal);
            for (int i = 0; i < len; i++) list.add(java.lang.reflect.Array.get(idsVal, i));
            iterable = list;
        } else {
            iterable = List.of(idsVal);
        }
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(Object::toString)
                .map(id -> LOCK_PREFIX + prefix + ":" + id)
                .collect(Collectors.toList());
    }
}
