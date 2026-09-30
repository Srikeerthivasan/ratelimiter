package com.app.ratelimit.ratelimiter.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.app.ratelimit.ratelimiter.core.RateLimitAlgorithm;
import com.app.ratelimit.ratelimiter.core.RateLimitConfig;
import com.app.ratelimit.ratelimiter.core.RateLimiter;
import com.app.ratelimit.ratelimiter.store.InMemoryRateLimitStateStore;
import com.app.ratelimit.ratelimiter.time.MutableTestClock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class SlidingWindowRateLimiterTest {
    private final MutableTestClock clock = new MutableTestClock(Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void grantsOnlyTheConfiguredCapacityWithinOneWindow() {
        RateLimiter limiter = newLimiter(5, Duration.ofSeconds(10));

        assertTrue(limiter.tryAcquire("client-a", 5).allowed());
        assertFalse(limiter.tryAcquire("client-a", 1).allowed());
    }

    @Test
    void expiresARequestAtTheExactWindowBoundary() {
        RateLimiter limiter = newLimiter(1, Duration.ofSeconds(10));
        assertTrue(limiter.tryAcquire("client-a", 1).allowed());

        clock.advanceBy(Duration.ofSeconds(9));
        assertFalse(limiter.tryAcquire("client-a", 1).allowed());

        clock.advanceBy(Duration.ofSeconds(1));
        assertTrue(limiter.tryAcquire("client-a", 1).allowed());
    }

    @Test
    void supportsBulkPermitRequests() {
        RateLimiter limiter = newLimiter(5, Duration.ofSeconds(10));

        assertEquals(2, limiter.tryAcquire("client-a", 3).remainingPermits());
        assertEquals(0, limiter.tryAcquire("client-a", 2).remainingPermits());
        assertFalse(limiter.tryAcquire("client-a", 1).allowed());
    }

    @Test
    void calculatesRetryAfterWhenMultiplePermitsMustExpire() {
        RateLimiter limiter = newLimiter(3, Duration.ofSeconds(10));
        limiter.tryAcquire("client-a", 1);
        clock.advanceBy(Duration.ofSeconds(1));
        limiter.tryAcquire("client-a", 1);
        clock.advanceBy(Duration.ofSeconds(1));
        limiter.tryAcquire("client-a", 1);

        clock.advanceBy(Duration.ofSeconds(1));
        assertEquals(8, limiter.tryAcquire("client-a", 2).retryAfterSeconds());
    }

    @Test
    void keepsClientWindowsIndependent() {
        RateLimiter limiter = newLimiter(1, Duration.ofSeconds(10));

        assertTrue(limiter.tryAcquire("client-a", 1).allowed());
        assertFalse(limiter.tryAcquire("client-a", 1).allowed());
        assertTrue(limiter.tryAcquire("client-b", 1).allowed());
    }

    @Test
    void rejectsRequestsThatCanNeverFitIntoTheWindow() {
        RateLimiter limiter = newLimiter(5, Duration.ofSeconds(10));

        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire("client-a", 6));
    }

    @Test
    void doesNotExceedCapacityUnderConcurrentRequests() throws Exception {
        RateLimiter limiter = newLimiter(5, Duration.ofSeconds(10));
        ExecutorService executor = Executors.newFixedThreadPool(20);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int index = 0; index < 20; index++) {
                tasks.add(() -> limiter.tryAcquire("client-a", 1).allowed());
            }

            List<Future<Boolean>> results = executor.invokeAll(tasks);
            long allowedRequests = results.stream().map(this::getUnchecked).filter(Boolean::booleanValue).count();
            assertEquals(5, allowedRequests);
        } finally {
            executor.shutdownNow();
        }
    }

    private RateLimiter newLimiter(long capacity, Duration windowDuration) {
        RateLimitConfig config = new RateLimitConfig(
                RateLimitAlgorithm.SLIDING_WINDOW,
                capacity,
                1,
                Duration.ofSeconds(10),
                windowDuration);
        return new SlidingWindowRateLimiter(config, clock, new InMemoryRateLimitStateStore<>());
    }

    private boolean getUnchecked(Future<Boolean> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError("Concurrent task failed", exception);
        }
    }
}
