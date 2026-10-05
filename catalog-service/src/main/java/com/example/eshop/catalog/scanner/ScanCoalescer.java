package com.example.eshop.catalog.scanner;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Collapses repeated camera reads of the same code. The window is display-only and is not a checkout cache. */
public final class ScanCoalescer {
    private final long windowMs;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Hit<?>> hits = new ConcurrentHashMap<>();

    public ScanCoalescer(long windowMs, LongSupplier clock) {
        this.windowMs = windowMs;
        this.clock = clock;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> loader) {
        long now = clock.getAsLong();
        Hit<?> current = hits.get(key);
        if (current != null && now - current.at <= windowMs) {
            return (T) current.value;
        }
        T value = loader.get();
        hits.put(key, new Hit<>(now, value));
        if (hits.size() > 2000) {
            hits.entrySet().removeIf(entry -> now - entry.getValue().at > windowMs);
        }
        return value;
    }

    private record Hit<T>(long at, T value) {}
}
