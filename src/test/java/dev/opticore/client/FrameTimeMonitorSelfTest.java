package dev.opticore.client;

/** Plain-JDK regression checks for the frame-time sampler. */
public final class FrameTimeMonitorSelfTest {
    private FrameTimeMonitorSelfTest() {}

    public static void main(String[] args) {
        FrameTimeMonitor.reset();
        check(FrameTimeMonitor.latestFrameTimeMs() == 0.0D, "reset must clear latest frame time");
        check(FrameTimeMonitor.onePercentLowFps() == 0.0D, "reset must clear percentile");
        String initialDisplay = FrameTimeMonitor.displayText();
        check(initialDisplay.equals("Frame time: -- ms | 1% low: -- FPS"),
                "reset must restore the placeholder HUD text");
        check(initialDisplay == FrameTimeMonitor.displayText(),
                "reading cached HUD text must not create a new string");

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
        check(FrameTimeMonitor.displayText().equals("Frame time: -- ms | 1% low: -- FPS"),
                "reset must clear cached display text");
        System.out.println("FrameTimeMonitorSelfTest: all checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
