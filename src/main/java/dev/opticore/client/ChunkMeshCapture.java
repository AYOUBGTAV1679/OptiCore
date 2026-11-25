package dev.opticore.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.opticore.cache.ImmutableMeshSnapshot;
import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.RenderType;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

/**
 * Narrow integration point between a live vanilla compilation result and the
 * immutable CPU mesh cache. Reusable storage remains disabled until exact
 * section identity and dependency fingerprints are available from the compiler.
 */
public final class ChunkMeshCapture {
    private static final LongAdder CAPTURED = new LongAdder();
    private static final LongAdder SKIPPED = new LongAdder();
    private static final LongAdder OBSERVED_BUFFERS = new LongAdder();
    private static final LongAdder COPIED_BYTES = new LongAdder();
    private static final long MAX_OBSERVATION_BYTES_PER_COMPILE = 4L * 1024L * 1024L;

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
        if (stored) CAPTURED.increment();
        else SKIPPED.increment();
        return stored;
    }

    /**
     * Copies each completed layer synchronously while vanilla's RenderedBuffer
     * is alive, then discards the copy. sectionY=0 is a diagnostic placeholder:
     * it must never be used for cache storage or mesh reuse.
     */
    public static void observeCompilation(
            Level level,
            int chunkX,
            int chunkZ,
            Map<RenderType, BufferBuilder.RenderedBuffer> layers) {
        if (level == null || !level.isClientSide() || layers == null) return;

        long compileBytes = 0L;
        for (BufferBuilder.RenderedBuffer rendered : layers.values()) {
            if (rendered == null || rendered.isEmpty()) continue;
            try {
                ByteBuffer vertexView = rendered.vertexBuffer().asReadOnlyBuffer();
                ByteBuffer indexView = rendered.indexBuffer().asReadOnlyBuffer();
                long payloadBytes = (long) vertexView.remaining() + indexView.remaining();
                if (payloadBytes <= 0L
                        || payloadBytes > MAX_OBSERVATION_BYTES_PER_COMPILE - compileBytes) {
                    SKIPPED.increment();
                    continue;
                }

                ImmutableMeshSnapshot snapshot =
                        RenderedBufferSnapshotCopier.copy(rendered, chunkX, 0, chunkZ);
                if (snapshot == null) {
                    SKIPPED.increment();
                    continue;
                }
                compileBytes += snapshot.sizeBytes();
                OBSERVED_BUFFERS.increment();
                COPIED_BYTES.add(snapshot.sizeBytes());
            } catch (RuntimeException invalidBuffer) {
                SKIPPED.increment();
            }
        }
    }

    public static long observedBufferCount() { return OBSERVED_BUFFERS.sum(); }
    public static long copiedBytes() { return COPIED_BYTES.sum(); }
    public static long capturedCount() { return CAPTURED.sum(); }
    public static long skippedCount() { return SKIPPED.sum(); }

    public static void resetCounters() {
        CAPTURED.reset();
        SKIPPED.reset();
        OBSERVED_BUFFERS.reset();
        COPIED_BYTES.reset();
    }
}
