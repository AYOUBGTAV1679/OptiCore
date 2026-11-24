package dev.opticore.client;

import dev.opticore.cache.CacheEntry;
import dev.opticore.cache.SmartChunkCache;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Client-side cache lifecycle bridge for renderer-owned disposable artifacts.
 *
 * This class deliberately does not cache live RenderChunk instances or GPU buffers.
 * The mesh compiler hook must supply immutable CPU-side artifacts before entries
 * are stored here.
 */
public final class ChunkRenderCache {
    private static final long DEFAULT_BUDGET_BYTES = 32L * 1024L * 1024L;
    private static final SmartChunkCache<CacheEntry> CACHE =
            new SmartChunkCache<>(DEFAULT_BUDGET_BYTES);

    private ChunkRenderCache() {}

    public static SmartChunkCache<CacheEntry> cache() {
        return CACHE;
    }

    public static void invalidateChunk(Level level, int chunkX, int chunkZ) {
        if (level == null || !level.isClientSide()) return;
        CACHE.invalidateChunk(dimensionId(level.dimension()), chunkX, chunkZ);
    }

    public static void invalidateDimension(ResourceKey<Level> dimension) {
        if (dimension != null) CACHE.invalidateDimension(dimensionId(dimension));
    }

    public static void invalidateAll() {
        CACHE.clear();
        CACHE.onResourcesReloaded();
    }

    public static String dimensionId(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }
}
