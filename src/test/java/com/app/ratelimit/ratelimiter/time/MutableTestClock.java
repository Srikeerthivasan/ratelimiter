package com.app.ratelimit.ratelimiter.time;

import java.time.Duration;
import java.time.Instant;

/**
 * Test-only clock. Use it in algorithm unit tests instead of Thread.sleep().
 */
public final class MutableTestClock implements Clock {
    private Instant currentTime;

    public MutableTestClock(Instant initialTime) {
        this.currentTime = initialTime;
    }

    @Override
    public Instant now() {
        return currentTime;
    }

    public void advanceBy(Duration duration) {
        currentTime = currentTime.plus(duration);
    }
}
