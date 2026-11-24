package dev.opticore.client;

import java.util.Arrays;

/**
 * Lightweight rolling frame-time sampler. Samples are collected on the render
 * thread and kept in a fixed-size ring buffer to avoid per-frame allocations.
 */
public final class FrameTimeMonitor {
    private static final int WINDOW_SIZE = 120;
    private static final long[] SAMPLES_NANOS = new long[WINDOW_SIZE];
    private static int nextIndex;
    private static int sampleCount;
    private static long previousFrameNanos;
    private static long latestFrameNanos;

    private FrameTimeMonitor() {}

    public static void sampleFrame() {
        long now = System.nanoTime();
        if (previousFrameNanos != 0L) {
            long elapsed = now - previousFrameNanos;
            if (elapsed > 0L) {
                latestFrameNanos = elapsed;
                SAMPLES_NANOS[nextIndex] = elapsed;
                nextIndex = (nextIndex + 1) % WINDOW_SIZE;
                if (sampleCount < WINDOW_SIZE) sampleCount++;
            }
        }
        previousFrameNanos = now;
    }

    public static double latestFrameTimeMs() {
        return latestFrameNanos / 1_000_000.0D;
    }

    /**
     * Returns a rolling 1% low estimate derived from the 99th percentile
     * frame time. Returns zero until at least two frames have been sampled.
     */
    public static double onePercentLowFps() {
        if (sampleCount < 2) return 0.0D;
        long[] sorted = new long[sampleCount];
        for (int i = 0; i < sampleCount; i++) sorted[i] = SAMPLES_NANOS[i];
        Arrays.sort(sorted);
        int percentileIndex = Math.min(sampleCount - 1,
                (int) Math.ceil(sampleCount * 0.99D) - 1);
        long percentileFrameNanos = sorted[percentileIndex];
        return percentileFrameNanos <= 0L ? 0.0D : 1_000_000_000.0D / percentileFrameNanos;
    }

    public static void reset() {
        previousFrameNanos = 0L;
        latestFrameNanos = 0L;
        nextIndex = 0;
        sampleCount = 0;
        Arrays.fill(SAMPLES_NANOS, 0L);
    }
}
