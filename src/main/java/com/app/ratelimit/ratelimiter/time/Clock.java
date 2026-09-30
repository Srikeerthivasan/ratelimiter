package com.app.ratelimit.ratelimiter.time;

import java.time.Instant;

/**
 * Provides time to algorithms so unit tests can use a controllable clock.
 */
public interface Clock {
    Instant now();
}
