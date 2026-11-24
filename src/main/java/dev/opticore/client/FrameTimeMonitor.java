package dev.opticore.client;

import java.util.Arrays;
import java.util.Locale;

/**
 * Rolling frame-time sampler. All methods are expected to be called on the
 * Minecraft render thread. Sampling uses fixed-size buffers and does not
 * allocate memory per frame. The HUD text is refreshed at most four times
 * per second so formatting does not add work to every rendered frame.
 */
public final class FrameTimeMonitor {
    private static final int WINDOW_SIZE = 120;
    private static final int PERCENTILE_REFRESH_INTERVAL = 15;
    private static final long DISPLAY_REFRESH_NANOS = 250_000_000L;
    private static final long[] SAMPLES_NANOS = new long[WINDOW_SIZE];
    private static final long[] SORT_BUFFER = new long[WINDOW_SIZE];

    private static int nextIndex;
    private static int sampleCount;
    private static int samplesSincePercentileRefresh;
    private static long previousFrameNanos;
    private static long latestFrameNanos;
    private static long lastDisplayRefreshNanos;
    private static double cachedOnePercentLowFps;
    private static String cachedDisplayText = "Frame time: -- ms | 1% low: -- FPS";

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
                samplesSincePercentileRefresh++;
                if (sampleCount >= 2 && (samplesSincePercentileRefresh >= PERCENTILE_REFRESH_INTERVAL
                        || cachedOnePercentLowFps == 0.0D)) {
                    refreshOnePercentLow();
                }
            }
        }
        previousFrameNanos = now;
        if (now - lastDisplayRefreshNanos >= DISPLAY_REFRESH_NANOS) {
            refreshDisplayText(now);
        }
    }

    public static double latestFrameTimeMs() {
        return latestFrameNanos / 1_000_000.0D;
    }

    /**
     * Returns a rolling estimate based on the 99th-percentile frame time.
     * The percentile is recalculated every 15 samples to avoid sorting on
     * every rendered frame.
     */
    public static double onePercentLowFps() {
        return cachedOnePercentLowFps;
    }

    /** Returns the last HUD string; formatting is throttled to 4 Hz. */
    public static String displayText() {
        return cachedDisplayText;
    }

    private static void refreshOnePercentLow() {
        System.arraycopy(SAMPLES_NANOS, 0, SORT_BUFFER, 0, sampleCount);
        Arrays.sort(SORT_BUFFER, 0, sampleCount);
        int percentileIndex = Math.min(sampleCount - 1,
                (sampleCount * 99 + 99) / 100 - 1);
        long percentileFrameNanos = SORT_BUFFER[percentileIndex];
        cachedOnePercentLowFps = percentileFrameNanos <= 0L
                ? 0.0D : 1_000_000_000.0D / percentileFrameNanos;
        samplesSincePercentileRefresh = 0;
    }

    private static void refreshDisplayText(long now) {
        String lowText = cachedOnePercentLowFps > 0.0D
                ? String.format(Locale.ROOT, "%.1f", cachedOnePercentLowFps) : "--";
        cachedDisplayText = String.format(Locale.ROOT,
                "Frame time: %.2f ms | 1%% low: %s FPS", latestFrameTimeMs(), lowText);
        lastDisplayRefreshNanos = now;
    }

    public static void reset() {
        previousFrameNanos = 0L;
        latestFrameNanos = 0L;
        lastDisplayRefreshNanos = 0L;
        cachedOnePercentLowFps = 0.0D;
        cachedDisplayText = "Frame time: -- ms | 1% low: -- FPS";
        nextIndex = 0;
        sampleCount = 0;
        samplesSincePercentileRefresh = 0;
        Arrays.fill(SAMPLES_NANOS, 0L);
        Arrays.fill(SORT_BUFFER, 0L);
    }
}
