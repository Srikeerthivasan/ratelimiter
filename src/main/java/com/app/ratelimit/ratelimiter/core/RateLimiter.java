package com.app.ratelimit.ratelimiter.core;

/**
 * Defines the contract implemented by every rate-limiting algorithm.
 */
public interface RateLimiter {

    /**
     * Attempts to grant permits for one rate-limit key.
     *
     * @param clientId the key whose limit is being evaluated
     * @param requestedPermits the number of permits to acquire
     * @return the outcome of the evaluation
     */
    RateLimitResult tryAcquire(String clientId, long requestedPermits);
}
