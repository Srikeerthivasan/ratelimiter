package com.app.ratelimit.ratelimiter.store;

import java.util.Optional;
import java.util.function.Supplier;

import com.app.ratelimit.ratelimiter.core.RateLimitResult;

/**
 * Storage boundary that can later be implemented with Redis or another shared store.
 */
public interface RateLimitStateStore<S> {
    S getOrCreate(String clientId, Supplier<S> stateFactory);

    Optional<S> find(String clientId);

    void remove(String clientId);

}
