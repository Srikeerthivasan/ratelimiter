package com.app.ratelimit.ratelimiter.algorithm;

import java.time.Instant;

/**
 * Mutable state for one client's token bucket. Its updates must be protected
 * by the atomic per-key operation chosen in TokenBucketRateLimiter.
 */
public final class TokenBucketState {
    private long availableTokens;
    private Instant lastRefillAt;

    public TokenBucketState(long availableTokens, Instant lastRefillAt) {
        this.availableTokens = availableTokens;
        this.lastRefillAt = lastRefillAt;
    }

    public long availableTokens() {
        return availableTokens;
    }

    public Instant lastRefillAt() {
        return lastRefillAt;
    }

    void update(long availableTokens, Instant lastRefillAt) {
        this.availableTokens = availableTokens;
        this.lastRefillAt = lastRefillAt;
    }
}
