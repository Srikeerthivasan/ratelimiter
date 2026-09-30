package com.app.ratelimit.ratelimiter.algorithm;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Mutable state for one client's sliding window. The deque records accepted
 * request timestamps and must not be modified outside an atomic per-key update.
 */
public final class SlidingWindowState {
    private final Deque<Instant> acceptedRequestTimes = new ArrayDeque<>();

    Deque<Instant> acceptedRequestTimes() {
        return acceptedRequestTimes;
    }
}
