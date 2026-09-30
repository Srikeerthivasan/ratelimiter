package com.app.ratelimit.ratelimiter.config;

import com.app.ratelimit.ratelimiter.core.RateLimiter;
import com.app.ratelimit.ratelimiter.factory.RateLimiterFactory;
import com.app.ratelimit.ratelimiter.time.Clock;
import com.app.ratelimit.ratelimiter.time.SystemClock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterConfiguration {
    @Bean
    Clock clock() {
        return new SystemClock();
    }

    @Bean
    RateLimiter rateLimiter(RateLimiterProperties properties, Clock clock) {
        return new RateLimiterFactory().create(properties.toConfig(), clock);
    }
}
