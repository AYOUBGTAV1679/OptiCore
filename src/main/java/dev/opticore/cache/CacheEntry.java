package dev.opticore.cache;

/**
 * A disposable, derived piece of rendering data. Implementations must not hold
 * references to live world state, entities, or gameplay objects.
 */
public interface CacheEntry {
    /** Approximate retained size in bytes; constant for the entry's lifetime. */
    long sizeBytes();

    /** Called once by the cache after removal. Must be safe to invoke off-thread. */
    default void release() {}
}