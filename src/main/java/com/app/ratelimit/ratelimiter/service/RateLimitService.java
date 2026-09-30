package com.app.ratelimit.ratelimiter.service;

import com.app.ratelimit.ratelimiter.core.RateLimitResult;
import com.app.ratelimit.ratelimiter.core.RateLimiter;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
    private final RateLimiter rateLimiter;

    public RateLimitService(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public RateLimitResult checkRateLimit(String clientId, long requestedPermits) {
        return rateLimiter.tryAcquire(clientId, requestedPermits);
    }
}
