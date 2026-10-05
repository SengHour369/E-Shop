package com.example.eshop.catalog.scanner;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Per-actor limit for distinct scanner lookups on this instance. */
public final class ScanRateLimiter {
    private final int limit;
    private final long windowMs;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public ScanRateLimiter(int limit, int windowSeconds, LongSupplier clock) {
        this.limit = limit;
        this.windowMs = windowSeconds * 1000L;
        this.clock = clock;
    }

    public void acquire(String actor) {
        long now = clock.getAsLong();
        Window updated = windows.compute(actor, (key, current) -> {
            if (current == null || now - current.start >= windowMs) {
                return new Window(now, 1, false);
            }
            if (current.count >= limit) {
                return new Window(current.start, current.count, true);
            }
            return new Window(current.start, current.count + 1, false);
        });
        if (updated.denied) {
            throw new ScanRateLimitedException();
        }
    }

    private record Window(long start, int count, boolean denied) {}
}
