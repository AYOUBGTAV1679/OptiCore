package dev.opticore.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.opticore.cache.ImmutableMeshSnapshot;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.RenderType;
import java.util.Map;

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
    private static final LongAdder OBSERVED_BUFFERS = new LongAdder();
    private static final LongAdder COPIED_BYTES = new LongAdder();

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


    /**
     * Observe and defensively copy every completed layer while vanilla's
     * RenderedBuffers are still alive. The copies are intentionally measured
     * and discarded for now: safe cache reuse remains disabled until the
     * compiler's exact neighbor/light/settings dependency fingerprints exist.
     */
    public static void observeCompilation(
            Level level,
            BlockPos renderChunkOrigin,
            Map<RenderType, BufferBuilder.RenderedBuffer> layers) {
        if (level == null || !level.isClientSide() || renderChunkOrigin == null || layers == null) {
            return;
        }

        int chunkX = renderChunkOrigin.getX() >> 4;
        int sectionY = renderChunkOrigin.getY() >> 4;
        int chunkZ = renderChunkOrigin.getZ() >> 4;

        for (BufferBuilder.RenderedBuffer rendered : layers.values()) {
            if (rendered == null || rendered.isEmpty()) continue;
            try {
                ImmutableMeshSnapshot snapshot =
                        RenderedBufferSnapshotCopier.copy(rendered, chunkX, sectionY, chunkZ);
                if (snapshot == null) {
                    SKIPPED.increment();
                    continue;
                }
                OBSERVED_BUFFERS.increment();
                COPIED_BYTES.add(snapshot.sizeBytes());
            } catch (RuntimeException invalidBuffer) {
                SKIPPED.increment();
            }
        }
    }

    public static long observedBufferCount() {
        return OBSERVED_BUFFERS.sum();
    }

    public static long copiedBytes() {
        return COPIED_BYTES.sum();
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
        OBSERVED_BUFFERS.reset();
        COPIED_BYTES.reset();
    }
}
