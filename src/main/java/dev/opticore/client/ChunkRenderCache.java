package dev.opticore.client;

import dev.opticore.cache.CacheEntry;
import dev.opticore.cache.ImmutableMeshSnapshot;
import dev.opticore.cache.ChunkBoundaryInvalidation;
import dev.opticore.cache.SmartChunkCache;
import dev.opticore.cache.RenderCacheKey;
import dev.opticore.OptiCoreConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
    private static final Map<Level, Long> WORLD_SESSIONS = new WeakHashMap<>();
    private static final AtomicLong NEXT_WORLD_SESSION = new AtomicLong(1L);

    private ChunkRenderCache() {}

    public static void applyConfiguredBudget() {
        long configuredBytes = OptiCoreConfig.RENDER_CACHE_BUDGET_MB.get() * MIB;
        if (CACHE.maxBytes() != configuredBytes) {
            CACHE.setMaxBytes(configuredBytes);
        }
    }

    public static RenderCacheKey key(Level level, int chunkX, int chunkZ, int sectionY) {
        if (level == null || !level.isClientSide()) return null;
        return new RenderCacheKey(dimensionId(level.dimension()), worldSession(level),
                chunkX, chunkZ, sectionY, 0L, 0L, 0L);
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
        return new RenderCacheKey(dimensionId(level.dimension()), worldSession(level),
                chunkX, chunkZ, sectionY, neighborStateFingerprint,
                lightFingerprint, renderSettingsFingerprint);
    }

    /** Drop the identity token after this exact client Level has been unloaded. */
    public static void forgetWorldSession(Level level) {
        if (level == null) return;
        synchronized (WORLD_SESSIONS) {
            WORLD_SESSIONS.remove(level);
        }
    }

    private static long worldSession(Level level) {
        synchronized (WORLD_SESSIONS) {
            return WORLD_SESSIONS.computeIfAbsent(level,
                    ignored -> NEXT_WORLD_SESSION.getAndIncrement());
        }
    }

    public static void onRenderSettingsChanged() {
        CACHE.onRenderSettingsChanged();
        CACHE.purgeStale();
    }

    public static SmartChunkCache<CacheEntry> cache() {
        return CACHE;
    }

    /**
     * Store an immutable CPU mesh snapshot under a dependency-complete key.
     * The caller must compute fingerprints from the same inputs used to build
     * the snapshot. This is a storage bridge only; it does not intercept
     * Minecraft's vanilla chunk compilation or enable mesh reuse by itself.
     */
    public static boolean storeMesh(Level level, int chunkX, int chunkZ, int sectionY,
            long neighborStateFingerprint, long lightFingerprint,
            long renderSettingsFingerprint, ImmutableMeshSnapshot mesh) {
        if (mesh == null) return false;
        RenderCacheKey key = meshKey(level, chunkX, chunkZ, sectionY,
                neighborStateFingerprint, lightFingerprint, renderSettingsFingerprint);
        return key != null && CACHE.put(key, mesh);
    }

    /**
     * Look up a CPU mesh only when all dependency fingerprints match.
     * No live Minecraft or GPU object is returned from this cache.
     */
    public static ImmutableMeshSnapshot findMesh(Level level, int chunkX, int chunkZ, int sectionY,
            long neighborStateFingerprint, long lightFingerprint,
            long renderSettingsFingerprint) {
        RenderCacheKey key = meshKey(level, chunkX, chunkZ, sectionY,
                neighborStateFingerprint, lightFingerprint, renderSettingsFingerprint);
        if (key == null) return null;
        CacheEntry entry = CACHE.get(key);
        return entry instanceof ImmutableMeshSnapshot mesh ? mesh : null;
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

        String dimension = dimensionId(level.dimension());
        for (ChunkBoundaryInvalidation.ChunkCoordinate coordinate
                : ChunkBoundaryInvalidation.affectedByBlockEdit(pos.getX(), pos.getZ())) {
            CACHE.invalidateChunk(dimension, coordinate.x(), coordinate.z());
        }
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
