package dev.opticore.client;

/** Render-thread-only zoom interpolation controller. */
public final class ZoomController {
    private static final double SMOOTHING_SECONDS = 0.085D;
    private static final double MAX_DELTA_SECONDS = 0.10D;

    private static double currentMultiplier = 1.0D;
    private static long previousUpdateNanos;

    private ZoomController() {}

    public static double update(boolean zoomHeld, double zoomFactor, boolean smooth) {
        long now = System.nanoTime();
        double target = zoomHeld ? zoomFactor : 1.0D;
        if (!smooth) {
            currentMultiplier = target;
            previousUpdateNanos = now;
            return currentMultiplier;
        }

        if (previousUpdateNanos == 0L) {
            previousUpdateNanos = now;
            currentMultiplier = target == 1.0D ? 1.0D : currentMultiplier;
            return currentMultiplier;
        }

        double deltaSeconds = Math.min(MAX_DELTA_SECONDS,
                Math.max(0.0D, (now - previousUpdateNanos) / 1_000_000_000.0D));
        previousUpdateNanos = now;
        double alpha = 1.0D - Math.exp(-deltaSeconds / SMOOTHING_SECONDS);
        currentMultiplier += (target - currentMultiplier) * alpha;
        if (Math.abs(target - currentMultiplier) < 0.0005D) {
            currentMultiplier = target;
        }
        return currentMultiplier;
    }

    public static void reset() {
        currentMultiplier = 1.0D;
        previousUpdateNanos = 0L;
    }
}
