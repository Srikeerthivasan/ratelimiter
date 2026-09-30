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

class TokenBucketRateLimiterTest {
    private final MutableTestClock clock = new MutableTestClock(Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void grantsOnlyTheConfiguredCapacity() {
        RateLimiter limiter = newLimiter(5, 2, Duration.ofSeconds(10));

        assertTrue(limiter.tryAcquire("client-a", 5).allowed());
        assertFalse(limiter.tryAcquire("client-a", 1).allowed());
    }

    @Test
    void refillsAtTheBoundaryAndPreservesPartialElapsedTime() {
        RateLimiter limiter = newLimiter(5, 2, Duration.ofSeconds(10));
        limiter.tryAcquire("client-a", 5);

        clock.advanceBy(Duration.ofSeconds(25));
        assertTrue(limiter.tryAcquire("client-a", 4).allowed());

        clock.advanceBy(Duration.ofSeconds(5));
        assertTrue(limiter.tryAcquire("client-a", 2).allowed());
    }

    @Test
    void calculatesRetryAfterFromTheNextRefillBoundary() {
        RateLimiter limiter = newLimiter(5, 2, Duration.ofSeconds(10));
        limiter.tryAcquire("client-a", 5);

        clock.advanceBy(Duration.ofSeconds(5));
        assertEquals(5, limiter.tryAcquire("client-a", 1).retryAfterSeconds());
    }

    @Test
    void keepsClientBucketsIndependent() {
        RateLimiter limiter = newLimiter(1, 1, Duration.ofSeconds(10));

        assertTrue(limiter.tryAcquire("client-a", 1).allowed());
        assertFalse(limiter.tryAcquire("client-a", 1).allowed());
        assertTrue(limiter.tryAcquire("client-b", 1).allowed());
    }

    @Test
    void rejectsRequestsThatCanNeverFitIntoTheBucket() {
        RateLimiter limiter = newLimiter(5, 1, Duration.ofSeconds(10));

        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire("client-a", 6));
    }

    @Test
    void doesNotOverspendUnderConcurrentRequests() throws Exception {
        RateLimiter limiter = newLimiter(5, 1, Duration.ofSeconds(10));
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

    private RateLimiter newLimiter(long capacity, long refillTokens, Duration refillPeriod) {
        RateLimitConfig config = new RateLimitConfig(
                RateLimitAlgorithm.TOKEN_BUCKET,
                capacity,
                refillTokens,
                refillPeriod,
                Duration.ofSeconds(10));
        return new TokenBucketRateLimiter(config, clock, new InMemoryRateLimitStateStore<>());
    }

    private boolean getUnchecked(Future<Boolean> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError("Concurrent task failed", exception);
        }
    }
}
