package com.app.ratelimit.ratelimiter.api;

import com.app.ratelimit.ratelimiter.core.RateLimitResult;
import com.app.ratelimit.ratelimiter.service.RateLimitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RateLimitController {
    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/rate-limit/check")
    public ResponseEntity<RateLimitResponse> checkRateLimit(@Valid @RequestBody RateLimitRequest request) {
        System.out.println("Received request: " + request);
        RateLimitResult result = rateLimitService.checkRateLimit(request.clientId(), request.permits());
        RateLimitResponse response = new RateLimitResponse(
                result.allowed(),
                request.clientId(),
                request.permits(),
                result.remainingPermits(),
                result.retryAfterSeconds(),
                result.algorithm(),
                result.evaluatedAt());

        if (result.allowed()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(429)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(result.retryAfterSeconds()))
                .body(response);
    }
}
