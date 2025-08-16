package kr.hhplus.be.server.common.lock;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    LockKey prefix();
    String ids();

    long ttlMillis() default 10_000;
    long waitTimeoutMillis() default 3_000;
    long retryIntervalMillis() default 50;
}
