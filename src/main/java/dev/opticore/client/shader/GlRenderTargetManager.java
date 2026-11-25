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
        TARGETS.put(key, created);
        trimToBudget();
        return created;
    }

    private static void trimToBudget() {
        while (TARGETS.size() > MAX_TARGETS || totalPixels() > MAX_TOTAL_PIXELS) {
            Iterator<Map.Entry<String, GlRenderTarget>> iterator = TARGETS.entrySet().iterator();
            if (!iterator.hasNext()) return;
            Map.Entry<String, GlRenderTarget> eldest = iterator.next();
            iterator.remove();
            eldest.getValue().close();
        }
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
        GlRenderTarget target = TARGETS.remove(key);
        if (target != null) target.close();
    }

    /** Releases all targets, for resource reload or client shutdown. */
    public static void clear() {
        RenderSystem.assertOnRenderThread();
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
