package com.app.ratelimit.ratelimiter.algorithm;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.app.ratelimit.ratelimiter.core.RateLimitAlgorithm;
import com.app.ratelimit.ratelimiter.core.RateLimitConfig;
import com.app.ratelimit.ratelimiter.core.RateLimitResult;
import com.app.ratelimit.ratelimiter.core.RateLimiter;
import com.app.ratelimit.ratelimiter.store.RateLimitStateStore;
import com.app.ratelimit.ratelimiter.time.Clock;

public final class TokenBucketRateLimiter implements RateLimiter {
    private final RateLimitConfig config;
    private final Clock clock;
    private final RateLimitStateStore<TokenBucketState> stateStore;

    public TokenBucketRateLimiter(
            RateLimitConfig config,
            Clock clock,
            RateLimitStateStore<TokenBucketState> stateStore) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore must not be null");
        validateConfig(config);
    }

    private void refillTokens(TokenBucketState state, Instant now) {
        Duration elapsed = Duration.between(state.lastRefillAt(), now);
        if (elapsed.isNegative()) {
            return;
        }

        long completedPeriods = elapsed.dividedBy(config.refillPeriod());
        if (completedPeriods == 0) {
            return;
        }

        // While full, no additional credit can be retained. Resetting this
        // timestamp prevents a later acquisition from immediately gaining all
        // elapsed refills that occurred while the bucket was already full.
        if (state.availableTokens() == config.capacity()) {
            state.update(config.capacity(), now);
            return;
        }

        long tokensNeededToFillBucket = config.capacity() - state.availableTokens();
        long periodsToFillBucket = divideAndRoundUp(tokensNeededToFillBucket, config.refillTokens());
        long newAvailableTokens;
        if (completedPeriods >= periodsToFillBucket) {
            newAvailableTokens = config.capacity();
        } else {
            // This multiplication cannot overflow: it is strictly less than the
            // number of tokens required to reach capacity.
            newAvailableTokens = state.availableTokens() + completedPeriods * config.refillTokens();
        }

        Instant newLastRefillAt = state.lastRefillAt().plus(config.refillPeriod().multipliedBy(completedPeriods));
        state.update(newAvailableTokens, newLastRefillAt);
    }

    private boolean canAcquire(TokenBucketState state, long requestedPermits) {
        return state.availableTokens() >= requestedPermits;
    }

    @Override
    public RateLimitResult tryAcquire(String clientId, long requestedPermits) {
        validateRequest(clientId, requestedPermits);
        Instant now = clock.now();
        TokenBucketState state = stateStore.getOrCreate(
                clientId,
                () -> new TokenBucketState(config.capacity(), now));

        // The map makes creation atomic; this lock makes the entire decision
        // atomic for this client without blocking other clients' buckets.
        synchronized (state) {
            refillTokens(state, now);

            if (canAcquire(state, requestedPermits)) {
                long remainingPermits = state.availableTokens() - requestedPermits;
                state.update(remainingPermits, state.lastRefillAt());
                return new RateLimitResult(
                        true,
                        remainingPermits,
                        0,
                        RateLimitAlgorithm.TOKEN_BUCKET,
                        now);
            }

            return new RateLimitResult(
                    false,
                    state.availableTokens(),
                    calculateRetryAfterSeconds(state, requestedPermits, now),
                    RateLimitAlgorithm.TOKEN_BUCKET,
                    now);
        }
    }

    private long calculateRetryAfterSeconds(TokenBucketState state, long requestedPermits, Instant now) {
        long missingTokens = requestedPermits - state.availableTokens();
        long refillPeriodsNeeded = divideAndRoundUp(missingTokens, config.refillTokens());
        Duration elapsedSinceLastRefill = Duration.between(state.lastRefillAt(), now);
        Duration untilNextRefill = config.refillPeriod().minus(elapsedSinceLastRefill);
        Duration wait = untilNextRefill.plus(config.refillPeriod().multipliedBy(refillPeriodsNeeded - 1));
        return durationToRoundedUpSeconds(wait);
    }

    private void validateRequest(String clientId, long requestedPermits) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        if (requestedPermits <= 0) {
            throw new IllegalArgumentException("requestedPermits must be greater than zero");
        }
        if (requestedPermits > config.capacity()) {
            throw new IllegalArgumentException("requestedPermits must not exceed bucket capacity");
        }
    }

    private static void validateConfig(RateLimitConfig config) {
        if (config.capacity() <= 0 || config.refillTokens() <= 0) {
            throw new IllegalArgumentException("capacity and refillTokens must be greater than zero");
        }
        if (config.refillPeriod() == null || config.refillPeriod().isZero() || config.refillPeriod().isNegative()) {
            throw new IllegalArgumentException("refillPeriod must be positive");
        }
    }

    private static long divideAndRoundUp(long dividend, long divisor) {
        return dividend / divisor + (dividend % divisor == 0 ? 0 : 1);
    }

    private static long durationToRoundedUpSeconds(Duration duration) {
        long wholeSeconds = duration.toSeconds();
        return duration.minusSeconds(wholeSeconds).isZero() ? wholeSeconds : wholeSeconds + 1;
    }
}
