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
                dispatcher.getFreeBufferCount(), dispatcher.getStats());
    }

    public static Snapshot snapshot() {
        return latest;
    }

    public static final class Snapshot {
        private static final Snapshot EMPTY = new Snapshot(0, 0, 0, "Unavailable");
        public final int queuedBuilds;
        public final int pendingUploads;
        public final int freeBuffers;
        public final String dispatcherStats;

        private Snapshot(int queuedBuilds, int pendingUploads, int freeBuffers, String dispatcherStats) {
            this.queuedBuilds = queuedBuilds;
            this.pendingUploads = pendingUploads;
            this.freeBuffers = freeBuffers;
            this.dispatcherStats = dispatcherStats;
        }
    }
}
