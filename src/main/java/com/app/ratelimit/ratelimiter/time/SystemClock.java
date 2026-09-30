package com.app.ratelimit.ratelimiter.time;

import java.time.Instant;

public final class SystemClock implements Clock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
