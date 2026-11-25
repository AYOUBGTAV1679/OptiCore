package dev.opticore.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.opticore.cache.ImmutableMeshSnapshot;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

/**
 * Narrow integration point between a live vanilla compilation result and the
 * immutable CPU mesh cache. Call only while the RenderedBuffer is valid and
 * before vanilla releases it.
 *
 * <p>This class intentionally does not install a Mixin or discover section
 * coordinates/fingerprints. The future RebuildTask hook must supply those
 * values from the exact compile operation, then call captureAndStore.</p>
 */
public final class ChunkMeshCapture {
    private static final LongAdder CAPTURED = new LongAdder();
    private static final LongAdder SKIPPED = new LongAdder();

    private ChunkMeshCapture() {}

    public static boolean captureAndStore(
            Level level,
            int chunkX,
            int chunkZ,
            int sectionY,
            long neighborStateFingerprint,
            long lightFingerprint,
            long renderSettingsFingerprint,
            BufferBuilder.RenderedBuffer rendered) {
        Objects.requireNonNull(rendered, "rendered");

        final ImmutableMeshSnapshot snapshot;
        try {
            snapshot = RenderedBufferSnapshotCopier.copy(
                    rendered, chunkX, sectionY, chunkZ);
        } catch (RuntimeException invalidBuffer) {
            SKIPPED.increment();
            return false;
        }

        if (snapshot == null) {
            SKIPPED.increment();
            return false;
        }

        boolean stored = ChunkRenderCache.storeMesh(
                level, chunkX, chunkZ, sectionY,
                neighborStateFingerprint, lightFingerprint,
                renderSettingsFingerprint, snapshot);
        if (stored) {
            CAPTURED.increment();
        } else {
            SKIPPED.increment();
        }
        return stored;
    }

    public static long capturedCount() {
        return CAPTURED.sum();
    }

    public static long skippedCount() {
        return SKIPPED.sum();
    }

    public static void resetCounters() {
        CAPTURED.reset();
        SKIPPED.reset();
    }
}
