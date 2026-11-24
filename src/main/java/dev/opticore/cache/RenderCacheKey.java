package dev.opticore.cache;

import java.util.Objects;

/** Plain-value key for one cached render artifact; deliberately Minecraft-independent. */
public record RenderCacheKey(String dimension, int chunkX, int chunkZ, int section) {
    public RenderCacheKey {
        Objects.requireNonNull(dimension, "dimension");
    }

    public boolean inChunk(String dim, int cx, int cz) {
        return chunkX == cx && chunkZ == cz && dimension.equals(dim);
    }
}