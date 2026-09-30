package com.app.ratelimit.ratelimiter.algorithm;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

import com.app.ratelimit.ratelimiter.core.RateLimitAlgorithm;
import com.app.ratelimit.ratelimiter.core.RateLimitConfig;
import com.app.ratelimit.ratelimiter.core.RateLimitResult;
import com.app.ratelimit.ratelimiter.core.RateLimiter;
import com.app.ratelimit.ratelimiter.store.RateLimitStateStore;
import com.app.ratelimit.ratelimiter.time.Clock;

public final class SlidingWindowRateLimiter implements RateLimiter {
    private final RateLimitConfig config;
    private final Clock clock;
    private final RateLimitStateStore<SlidingWindowState> stateStore;

    public SlidingWindowRateLimiter(
            RateLimitConfig config,
            Clock clock,
            RateLimitStateStore<SlidingWindowState> stateStore) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore must not be null");
        validateConfig(config);
    }

    @Override
    public RateLimitResult tryAcquire(String clientId, long requestedPermits) {
        validateRequest(clientId, requestedPermits);
        Instant now = clock.now();
        SlidingWindowState state = stateStore.getOrCreate(clientId, SlidingWindowState::new);

        // State creation is atomic in the store. This per-client lock makes
        // expiry, capacity checking, and recording one atomic decision.
        synchronized (state) {
            removeExpiredRequests(state, now);

            if (canAcquire(state, requestedPermits)) {
                recordAcceptedRequests(state, requestedPermits, now);
                return new RateLimitResult(
                        true,
                        config.capacity() - state.acceptedRequestTimes().size(),
                        0,
                        RateLimitAlgorithm.SLIDING_WINDOW,
                        now);
            }

            return new RateLimitResult(
                    false,
                    config.capacity() - state.acceptedRequestTimes().size(),
                    calculateRetryAfterSeconds(state, requestedPermits, now),
                    RateLimitAlgorithm.SLIDING_WINDOW,
                    now);
        }
    }

    private void removeExpiredRequests(SlidingWindowState state, Instant now) {
        Instant cutoff = now.minus(config.windowDuration());
        Deque<Instant> requestTimes = state.acceptedRequestTimes();
        while (!requestTimes.isEmpty() && !requestTimes.peekFirst().isAfter(cutoff)) {
            requestTimes.removeFirst();
        }
    }

    private boolean canAcquire(SlidingWindowState state, long requestedPermits) {
        return state.acceptedRequestTimes().size() + requestedPermits <= config.capacity();
    }

    private void recordAcceptedRequests(SlidingWindowState state, long requestedPermits, Instant now) {
        Deque<Instant> requestTimes = state.acceptedRequestTimes();
        for (long permit = 0; permit < requestedPermits; permit++) {
            requestTimes.addLast(now);
        }
    }

    private long calculateRetryAfterSeconds(SlidingWindowState state, long requestedPermits, Instant now) {
        long permitsThatMustExpire = state.acceptedRequestTimes().size() + requestedPermits - config.capacity();
        Iterator<Instant> timestamps = state.acceptedRequestTimes().iterator();
        Instant finalRequiredExpiry = null;

        for (long permit = 0; permit < permitsThatMustExpire; permit++) {
            finalRequiredExpiry = timestamps.next().plus(config.windowDuration());
        }

        return durationToRoundedUpSeconds(Duration.between(now, finalRequiredExpiry));
    }

    private void validateRequest(String clientId, long requestedPermits) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        if (requestedPermits <= 0) {
            throw new IllegalArgumentException("requestedPermits must be greater than zero");
        }
        if (requestedPermits > config.capacity()) {
            throw new IllegalArgumentException("requestedPermits must not exceed window capacity");
        }
    }

    private static void validateConfig(RateLimitConfig config) {
        if (config.capacity() <= 0 || config.capacity() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("capacity must be between 1 and " + Integer.MAX_VALUE);
        }
        if (config.windowDuration() == null || config.windowDuration().isZero() || config.windowDuration().isNegative()) {
            throw new IllegalArgumentException("windowDuration must be positive");
        }
    }

    private static long durationToRoundedUpSeconds(Duration duration) {
        long wholeSeconds = duration.toSeconds();
        return duration.minusSeconds(wholeSeconds).isZero() ? wholeSeconds : wholeSeconds + 1;
    }
}
