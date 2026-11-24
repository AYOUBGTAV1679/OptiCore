package dev.opticore.client;

/** Plain-JDK regression checks for the frame-time sampler. */
public final class FrameTimeMonitorSelfTest {
    private FrameTimeMonitorSelfTest() {}

    public static void main(String[] args) {
        FrameTimeMonitor.reset();
        check(FrameTimeMonitor.latestFrameTimeMs() == 0.0D, "reset must clear latest frame time");
        check(FrameTimeMonitor.onePercentLowFps() == 0.0D, "reset must clear percentile");

        for (int i = 0; i < 180; i++) {
            FrameTimeMonitor.sampleFrame();
        }

        check(FrameTimeMonitor.latestFrameTimeMs() > 0.0D,
                "sampling must record a positive frame time");
        double onePercentLow = FrameTimeMonitor.onePercentLowFps();
        check(Double.isFinite(onePercentLow) && onePercentLow > 0.0D,
                "sampling must produce a finite positive 1% low estimate");

        FrameTimeMonitor.reset();
        check(FrameTimeMonitor.latestFrameTimeMs() == 0.0D, "reset must clear latest sample");
        check(FrameTimeMonitor.onePercentLowFps() == 0.0D, "reset must clear cached 1% low");
        System.out.println("FrameTimeMonitorSelfTest: all checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
