package com.app.ratelimit.ratelimiter.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * JSON payload accepted by the rate-limit endpoint.
 */
public record RateLimitRequest(
        @NotBlank(message = "clientId must not be blank") String clientId,
        @Min(value = 1, message = "permits must be at least 1")
        @Max(value = 1_000, message = "permits must not exceed 1000") long permits) {
}
