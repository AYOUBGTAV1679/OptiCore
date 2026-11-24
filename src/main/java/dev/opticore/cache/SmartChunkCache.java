package dev.opticore.cache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

/** Bounded, thread-safe LRU cache for disposable chunk render data. */
public final class SmartChunkCache<V extends CacheEntry> {
    private record Slot<V>(V value, long bytes, long resourceGen, long settingsGen) {}

    private final ReentrantLock lock = new ReentrantLock();
    private final LinkedHashMap<RenderCacheKey, Slot<V>> map = new LinkedHashMap<>(64, 0.75f, true);
    private long maxBytes, currentBytes, resourceGen, settingsGen;
    private final AtomicLong hits = new AtomicLong(), misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong(), invalidations = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong(), releaseFailures = new AtomicLong();

    public SmartChunkCache(long maxBytes) {
        if (maxBytes < 0) throw new IllegalArgumentException("maxBytes < 0");
        this.maxBytes = maxBytes;
    }

    public V get(RenderCacheKey key) {
        Objects.requireNonNull(key, "key");
        V stale = null, found = null;
        lock.lock();
        try {
            Slot<V> slot = map.get(key);
            if (slot != null) {
                if (slot.resourceGen != resourceGen || slot.settingsGen != settingsGen) {
                    map.remove(key); currentBytes -= slot.bytes;
                    invalidations.incrementAndGet(); stale = slot.value;
                } else found = slot.value;
            }
        } finally { lock.unlock(); }
        if (stale != null) safeRelease(stale);
        if (found != null) hits.incrementAndGet(); else misses.incrementAndGet();
        return found;
    }

    public boolean put(RenderCacheKey key, V value) {
        Objects.requireNonNull(key, "key"); Objects.requireNonNull(value, "value");
        long bytes = Math.max(0L, value.sizeBytes());
        List<V> release = new ArrayList<>(2);
        boolean stored;
        lock.lock();
        try {
            Slot<V> old = map.remove(key);
            if (old != null) {
                currentBytes -= old.bytes;
                if (old.value != value) release.add(old.value);
            }
            if (bytes > maxBytes) {
                rejected.incrementAndGet();
                if (old == null || old.value != value) release.add(value);
                stored = false;
            } else {
                // Make room before adding so currentBytes + bytes cannot overflow
                // even when callers configure a budget near Long.MAX_VALUE.
                evictUntilFits(bytes, release);
                map.put(key, new Slot<>(value, bytes, resourceGen, settingsGen));
                currentBytes += bytes;
                stored = true;
            }
        } finally { lock.unlock(); }
        release.forEach(this::safeRelease);
        return stored;
    }

    public void setMaxBytes(long newMax) {
        if (newMax < 0) throw new IllegalArgumentException("maxBytes < 0");
        List<V> release = new ArrayList<>();
        lock.lock();
        try { maxBytes = newMax; evictToLimit(release); }
        finally { lock.unlock(); }
        release.forEach(this::safeRelease);
    }

    public int invalidateChunk(String dimension, int chunkX, int chunkZ) {
        return removeIf(k -> k.inChunk(dimension, chunkX, chunkZ));
    }
    public int invalidateDimension(String dimension) {
        return removeIf(k -> k.dimension().equals(dimension));
    }
    public void onResourcesReloaded() {
        lock.lock(); try { resourceGen++; } finally { lock.unlock(); }
    }
    public void onRenderSettingsChanged() {
        lock.lock(); try { settingsGen++; } finally { lock.unlock(); }
    }

    public int purgeStale() {
        List<V> release = new ArrayList<>();
        lock.lock();
        try {
            Iterator<Map.Entry<RenderCacheKey, Slot<V>>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Slot<V> slot = it.next().getValue();
                if (slot.resourceGen != resourceGen || slot.settingsGen != settingsGen) {
                    it.remove(); currentBytes -= slot.bytes; release.add(slot.value);
                }
            }
            invalidations.addAndGet(release.size());
        } finally { lock.unlock(); }
        release.forEach(this::safeRelease);
        return release.size();
    }

    public void clear() { removeIf(k -> true); }
    public long currentBytes() { lock.lock(); try { return currentBytes; } finally { lock.unlock(); } }
    public long maxBytes() { lock.lock(); try { return maxBytes; } finally { lock.unlock(); } }
    public int size() { lock.lock(); try { return map.size(); } finally { lock.unlock(); } }
    public Stats stats() {
        return new Stats(hits.get(), misses.get(), evictions.get(), invalidations.get(),
                rejected.get(), releaseFailures.get());
    }
    public record Stats(long hits, long misses, long evictions, long invalidations,
                        long rejected, long releaseFailures) {}

    private int removeIf(Predicate<RenderCacheKey> filter) {
        List<V> release = new ArrayList<>();
        lock.lock();
        try {
            Iterator<Map.Entry<RenderCacheKey, Slot<V>>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<RenderCacheKey, Slot<V>> entry = it.next();
                if (filter.test(entry.getKey())) {
                    it.remove(); currentBytes -= entry.getValue().bytes;
                    release.add(entry.getValue().value);
                }
            }
            invalidations.addAndGet(release.size());
        } finally { lock.unlock(); }
        release.forEach(this::safeRelease);
        return release.size();
    }

    /** Caller must hold the cache lock; bytes must be <= maxBytes. */
    private void evictUntilFits(long bytes, List<V> release) {
        long available = maxBytes - bytes;
        Iterator<Map.Entry<RenderCacheKey, Slot<V>>> it = map.entrySet().iterator();
        while (currentBytes > available && it.hasNext()) {
            Slot<V> slot = it.next().getValue();
            it.remove();
            currentBytes -= slot.bytes;
            evictions.incrementAndGet();
            release.add(slot.value);
        }
    }

    /** Caller must hold the cache lock. */
    private void evictToLimit(List<V> release) {
        Iterator<Map.Entry<RenderCacheKey, Slot<V>>> it = map.entrySet().iterator();
        while (currentBytes > maxBytes && it.hasNext()) {
            Slot<V> slot = it.next().getValue();
            it.remove(); currentBytes -= slot.bytes;
            evictions.incrementAndGet(); release.add(slot.value);
        }
    }
    private void safeRelease(V value) {
        try { value.release(); }
        catch (Throwable ignored) { releaseFailures.incrementAndGet(); }
    }
}