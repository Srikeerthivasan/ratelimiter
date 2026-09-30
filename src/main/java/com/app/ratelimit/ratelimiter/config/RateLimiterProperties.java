package com.app.ratelimit.ratelimiter.config;

import com.app.ratelimit.ratelimiter.core.RateLimitAlgorithm;
import com.app.ratelimit.ratelimiter.core.RateLimitConfig;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {
    private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;

    @Min(1)
    private long capacity = 5;

    @Min(1)
    private long refillTokens = 5;

    private Duration refillPeriod = Duration.ofSeconds(10);
    private Duration windowDuration = Duration.ofSeconds(10);

    public RateLimitConfig toConfig() {
        return new RateLimitConfig(algorithm, capacity, refillTokens, refillPeriod, windowDuration);
    }

    public RateLimitAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(long refillTokens) {
        this.refillTokens = refillTokens;
    }

    public Duration getRefillPeriod() {
        return refillPeriod;
    }

    public void setRefillPeriod(Duration refillPeriod) {
        this.refillPeriod = refillPeriod;
    }

    public Duration getWindowDuration() {
        return windowDuration;
    }

    public void setWindowDuration(Duration windowDuration) {
        this.windowDuration = windowDuration;
    }
}
