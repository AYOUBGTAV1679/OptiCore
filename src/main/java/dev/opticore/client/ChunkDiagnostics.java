package dev.opticore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

/** Read-only snapshot of vanilla's chunk compilation/upload queues. */
public final class ChunkDiagnostics {
    private static final int SAMPLE_INTERVAL_TICKS = 10;
    private static int ticksUntilSample;
    private static volatile Snapshot latest = Snapshot.EMPTY;

    private ChunkDiagnostics() {}

    public static void tick(Minecraft minecraft) {
        if (ticksUntilSample-- > 0) return;
        ticksUntilSample = SAMPLE_INTERVAL_TICKS;
        if (minecraft.level == null || minecraft.levelRenderer == null) {
            latest = Snapshot.EMPTY;
            return;
        }
        ChunkRenderDispatcher dispatcher = minecraft.levelRenderer.getChunkRenderDispatcher();
        if (dispatcher == null) {
            latest = Snapshot.EMPTY;
            return;
        }
        latest = new Snapshot(dispatcher.getToBatchCount(), dispatcher.getToUpload(),
                dispatcher.getFreeBufferCount(), dispatcher.getStats(), ChunkRenderCache.cache().stats(),
                ChunkRenderCache.cache().currentBytes(), ChunkRenderCache.cache().maxBytes(),
                ChunkMeshCapture.observedBufferCount(), ChunkMeshCapture.copiedBytes(),
                ChunkMeshCapture.skippedCount());
    }

    public static Snapshot snapshot() {
        return latest;
    }

    public static final class Snapshot {
        private static final Snapshot EMPTY = new Snapshot(0, 0, 0, "Unavailable",
                new dev.opticore.cache.SmartChunkCache.Stats(0, 0, 0, 0, 0, 0), 0, 0, 0, 0, 0);
        public final int queuedBuilds;
        public final int pendingUploads;
        public final int freeBuffers;
        public final String dispatcherStats;
        public final dev.opticore.cache.SmartChunkCache.Stats cacheStats;
        public final long cacheBytes;
        public final long cacheBudgetBytes;
        public final long observedMeshBuffers;
        public final long copiedMeshBytes;
        public final long skippedMeshBuffers;

        private Snapshot(int queuedBuilds, int pendingUploads, int freeBuffers, String dispatcherStats,
                         dev.opticore.cache.SmartChunkCache.Stats cacheStats, long cacheBytes, long cacheBudgetBytes,
                         long observedMeshBuffers, long copiedMeshBytes, long skippedMeshBuffers) {
            this.queuedBuilds = queuedBuilds;
            this.pendingUploads = pendingUploads;
            this.freeBuffers = freeBuffers;
            this.dispatcherStats = dispatcherStats;
            this.cacheStats = cacheStats;
            this.cacheBytes = cacheBytes;
            this.cacheBudgetBytes = cacheBudgetBytes;
            this.observedMeshBuffers = observedMeshBuffers;
            this.copiedMeshBytes = copiedMeshBytes;
            this.skippedMeshBuffers = skippedMeshBuffers;
        }
    }
}
