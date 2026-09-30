package com.app.ratelimit.ratelimiter.core;

import java.time.Duration;

/**
 * Algorithm-independent configuration supplied when a limiter is created.
 */
public record RateLimitConfig(
        RateLimitAlgorithm algorithm,
        long capacity,
        long refillTokens,
        Duration refillPeriod,
        Duration windowDuration) {
}
