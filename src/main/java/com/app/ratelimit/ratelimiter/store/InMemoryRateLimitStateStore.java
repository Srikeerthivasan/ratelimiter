package com.app.ratelimit.ratelimiter.store;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class InMemoryRateLimitStateStore<S> implements RateLimitStateStore<S> {
    private final ConcurrentHashMap<String, S> states = new ConcurrentHashMap<>();

    @Override
    public S getOrCreate(String clientId, Supplier<S> stateFactory) {
        return states.computeIfAbsent(clientId, ignored -> stateFactory.get());
    }

    @Override
    public Optional<S> find(String clientId) {
        return Optional.ofNullable(states.get(clientId));
    }

    @Override
    public void remove(String clientId) {
        states.remove(clientId);
    }
}
