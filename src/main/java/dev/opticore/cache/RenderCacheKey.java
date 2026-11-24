package dev.opticore.cache;

import java.util.Objects;

/**
 * Immutable identity for a derived render artifact.
 *
 * The fingerprints represent geometry dependencies sampled by a future mesh
 * producer. The legacy four-argument constructor remains for lifecycle-only
 * callers; it intentionally supplies zero fingerprints and is not sufficient
 * by itself to authorize mesh reuse.
 */
public record RenderCacheKey(
        String dimension,
        int chunkX,
        int chunkZ,
        int section,
        long neighborStateFingerprint,
        long lightFingerprint,
        long renderSettingsFingerprint) {

    public RenderCacheKey {
        Objects.requireNonNull(dimension, "dimension");
    }

    /** Lifecycle-only key; a mesh producer should use the full constructor. */
    public RenderCacheKey(String dimension, int chunkX, int chunkZ, int section) {
        this(dimension, chunkX, chunkZ, section, 0L, 0L, 0L);
    }

    public boolean inChunk(String dim, int cx, int cz) {
        return chunkX == cx && chunkZ == cz && dimension.equals(dim);
    }
}
