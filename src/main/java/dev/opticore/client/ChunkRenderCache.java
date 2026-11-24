package dev.opticore.client;

import dev.opticore.cache.CacheEntry;
import dev.opticore.cache.SmartChunkCache;
import dev.opticore.cache.RenderCacheKey;
import dev.opticore.OptiCoreConfig;
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
    private static final long MIB = 1024L * 1024L;
    private static final SmartChunkCache<CacheEntry> CACHE =
            new SmartChunkCache<>(32L * MIB);

    private ChunkRenderCache() {}

    public static void applyConfiguredBudget() {
        long configuredBytes = OptiCoreConfig.RENDER_CACHE_BUDGET_MB.get() * MIB;
        if (CACHE.maxBytes() != configuredBytes) {
            CACHE.setMaxBytes(configuredBytes);
        }
    }

    public static RenderCacheKey key(Level level, int chunkX, int chunkZ, int sectionY) {
        if (level == null || !level.isClientSide()) return null;
        return new RenderCacheKey(dimensionId(level.dimension()), chunkX, chunkZ, sectionY);
    }

    /**
     * Build a dependency-complete key for a future CPU mesh artifact producer.
     * Fingerprints must be derived from the exact neighbor block-state, light,
     * and render-setting inputs used to compile the section. This method does
     * not itself hook or alter Minecraft's mesh compilation path.
     */
    public static RenderCacheKey meshKey(Level level, int chunkX, int chunkZ, int sectionY,
            long neighborStateFingerprint, long lightFingerprint,
            long renderSettingsFingerprint) {
        if (level == null || !level.isClientSide()) return null;
        return new RenderCacheKey(dimensionId(level.dimension()), chunkX, chunkZ, sectionY,
                neighborStateFingerprint, lightFingerprint, renderSettingsFingerprint);
    }

    public static void onRenderSettingsChanged() {
        CACHE.onRenderSettingsChanged();
        CACHE.purgeStale();
    }

    public static SmartChunkCache<CacheEntry> cache() {
        return CACHE;
    }

    public static void invalidateChunk(Level level, int chunkX, int chunkZ) {
        if (level == null || !level.isClientSide()) return;
        CACHE.invalidateChunk(dimensionId(level.dimension()), chunkX, chunkZ);
    }

    /**
     * A block edit can change the visible face geometry of a neighboring chunk
     * when it occurs on a horizontal chunk border. Invalidate both sides.
     */
    public static void invalidateBlockAndNeighbors(Level level,
            net.minecraft.core.BlockPos pos) {
        if (level == null || pos == null || !level.isClientSide()) return;

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;

        invalidateChunk(level, chunkX, chunkZ);
        if (localX == 0) invalidateChunk(level, chunkX - 1, chunkZ);
        if (localX == 15) invalidateChunk(level, chunkX + 1, chunkZ);
        if (localZ == 0) invalidateChunk(level, chunkX, chunkZ - 1);
        if (localZ == 15) invalidateChunk(level, chunkX, chunkZ + 1);
    }

    public static void invalidateDimension(ResourceKey<Level> dimension) {
        if (dimension != null) CACHE.invalidateDimension(dimensionId(dimension));
    }

    public static void onResourcesReloaded() {
        CACHE.onResourcesReloaded();
        CACHE.purgeStale();
    }

    public static void invalidateAll() {
        CACHE.clear();
        CACHE.onResourcesReloaded();
    }

    public static String dimensionId(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }
}
