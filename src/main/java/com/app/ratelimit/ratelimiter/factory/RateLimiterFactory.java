package com.app.ratelimit.ratelimiter.factory;

import com.app.ratelimit.ratelimiter.algorithm.SlidingWindowRateLimiter;
import com.app.ratelimit.ratelimiter.algorithm.SlidingWindowState;
import com.app.ratelimit.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.app.ratelimit.ratelimiter.algorithm.TokenBucketState;
import com.app.ratelimit.ratelimiter.core.RateLimitConfig;
import com.app.ratelimit.ratelimiter.core.RateLimiter;
import com.app.ratelimit.ratelimiter.store.InMemoryRateLimitStateStore;
import com.app.ratelimit.ratelimiter.time.Clock;

/**
 * Creates the rate-limiter strategy selected by configuration.
 */
public final class RateLimiterFactory {
    public RateLimiter create(RateLimitConfig config, Clock clock) {
        return switch (config.algorithm()) {
            case TOKEN_BUCKET -> new TokenBucketRateLimiter(
                    config, clock, new InMemoryRateLimitStateStore<TokenBucketState>());
            case SLIDING_WINDOW -> new SlidingWindowRateLimiter(
                    config, clock, new InMemoryRateLimitStateStore<SlidingWindowState>());
        };
    }
}
