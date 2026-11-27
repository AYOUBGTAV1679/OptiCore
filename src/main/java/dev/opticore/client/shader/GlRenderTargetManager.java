package dev.opticore.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded LRU owner for off-screen targets. All calls require the render
 * thread and a current OpenGL context.
 */
public final class GlRenderTargetManager {
    private static final int MAX_TARGETS = 4;
    private static final long MAX_TOTAL_PIXELS = 16_777_216L;
    private static final LinkedHashMap<String, GlRenderTarget> TARGETS =
            new LinkedHashMap<>(8, 0.75f, true);

    private GlRenderTargetManager() {}

    /**
     * Gets or creates a named target. Existing targets are resized
     * transactionally; failed allocations leave the previous target intact.
     */
    public static GlRenderTarget acquire(String key, int width, int height) {
        RenderSystem.assertOnRenderThread();
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new IllegalArgumentException("Render target key must contain 1-128 characters");
        }
        long requestedPixels = (long) width * height;
        if (width < 1 || height < 1 || requestedPixels > MAX_TOTAL_PIXELS) {
            throw new IllegalArgumentException("Requested render target exceeds manager budget");
        }

        GlRenderTarget existing = TARGETS.get(key);
        if (existing != null) {
            long otherPixels = totalPixels() - (long) existing.width() * existing.height();
            if (otherPixels + requestedPixels > MAX_TOTAL_PIXELS) {
                throw new IllegalArgumentException("Render target resize exceeds total pixel budget");
            }
            existing.resize(width, height);
            return existing;
        }

        // Allocate before evicting so an allocation failure does not destroy useful cached targets.
        GlRenderTarget created = GlRenderTarget.create(width, height);
        long projectedPixels = totalPixels() + requestedPixels;
        int projectedCount = TARGETS.size() + 1;
        java.util.List<String> evictionKeys = new java.util.ArrayList<>();

        // Plan all evictions first. Never partially evict the cache and then
        // discover that every remaining target is currently bound.
        for (Map.Entry<String, GlRenderTarget> entry : TARGETS.entrySet()) {
            if (projectedCount <= MAX_TARGETS && projectedPixels <= MAX_TOTAL_PIXELS) break;
            if (entry.getValue().isBound()) continue;
            evictionKeys.add(entry.getKey());
            projectedCount--;
            projectedPixels -= (long) entry.getValue().width() * entry.getValue().height();
        }
        if (projectedCount > MAX_TARGETS || projectedPixels > MAX_TOTAL_PIXELS) {
            created.close();
            throw new IllegalStateException("Render-target budget cannot be met without evicting a bound target");
        }

        for (String evictionKey : evictionKeys) {
            GlRenderTarget evicted = TARGETS.get(evictionKey);
            if (evicted != null) {
                evicted.close();
                TARGETS.remove(evictionKey);
            }
        }
        TARGETS.put(key, created);
        return created;
    }

    private static long totalPixels() {
        long total = 0;
        for (GlRenderTarget target : TARGETS.values()) {
            total += (long) target.width() * target.height();
        }
        return total;
    }

    public static void release(String key) {
        RenderSystem.assertOnRenderThread();
        GlRenderTarget target = TARGETS.get(key);
        if (target != null) {
            // Do not orphan a still-bound GL object by removing it first.
            target.close();
            TARGETS.remove(key);
        }
    }

    /** Releases all targets, for resource reload or client shutdown. */
    public static void clear() {
        RenderSystem.assertOnRenderThread();
        // Preflight all targets so a bound target cannot leave a partially
        // cleared manager with closed entries still registered.
        for (GlRenderTarget target : TARGETS.values()) {
            if (target.isBound()) {
                throw new IllegalStateException("Cannot clear render targets while one is bound");
            }
        }
        for (GlRenderTarget target : TARGETS.values()) target.close();
        TARGETS.clear();
    }

    public static int size() {
        RenderSystem.assertOnRenderThread();
        return TARGETS.size();
    }

    public static long allocatedPixels() {
        RenderSystem.assertOnRenderThread();
        return totalPixels();
    }
}
