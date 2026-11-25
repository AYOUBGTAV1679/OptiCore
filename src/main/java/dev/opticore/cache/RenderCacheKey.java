package dev.opticore.cache;

import java.util.Objects;

/**
 * Immutable identity for a derived render artifact.
 *
 * worldSession separates different client Level instances that share the same
 * dimension identifier. The fingerprints represent geometry dependencies
 * sampled by a future mesh producer.
 */
public record RenderCacheKey(
        String dimension,
        long worldSession,
        int chunkX,
        int chunkZ,
        int section,
        long neighborStateFingerprint,
        long lightFingerprint,
        long renderSettingsFingerprint) {

    public RenderCacheKey {
        Objects.requireNonNull(dimension, "dimension");
    }

    /** Compatibility constructor for lifecycle-only and isolated cache callers. */
    public RenderCacheKey(String dimension, int chunkX, int chunkZ, int section,
            long neighborStateFingerprint, long lightFingerprint,
            long renderSettingsFingerprint) {
        this(dimension, 0L, chunkX, chunkZ, section, neighborStateFingerprint,
                lightFingerprint, renderSettingsFingerprint);
    }

    /** Lifecycle-only key; a mesh producer should use a session-aware full key. */
    public RenderCacheKey(String dimension, int chunkX, int chunkZ, int section) {
        this(dimension, 0L, chunkX, chunkZ, section, 0L, 0L, 0L);
    }

    public boolean inChunk(String dim, int cx, int cz) {
        return chunkX == cx && chunkZ == cz && dimension.equals(dim);
    }
}
