package com.app.ratelimit.ratelimiter.core;

import java.time.Instant;

/**
 * Immutable outcome returned by a rate-limiting algorithm.
 */
public record RateLimitResult(
        boolean allowed,
        long remainingPermits,
        long retryAfterSeconds,
        RateLimitAlgorithm algorithm,
        Instant evaluatedAt) {
}
