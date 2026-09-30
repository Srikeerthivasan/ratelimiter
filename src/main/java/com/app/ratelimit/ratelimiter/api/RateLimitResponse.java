package com.app.ratelimit.ratelimiter.api;

import com.app.ratelimit.ratelimiter.core.RateLimitAlgorithm;
import java.time.Instant;

/**
 * JSON payload returned after a rate-limit evaluation.
 */
public record RateLimitResponse(
        boolean allowed,
        String clientId,
        long requestedPermits,
        long remainingPermits,
        long retryAfterSeconds,
        RateLimitAlgorithm algorithm,
        Instant evaluatedAt) {
}
