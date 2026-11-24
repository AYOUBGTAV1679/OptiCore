package dev.opticore.client;

/** Plain-JDK regression checks for zoom state transitions. */
public final class ZoomControllerSelfTest {
    private ZoomControllerSelfTest() {}

    public static void main(String[] args) {
        ZoomController.reset();
        double immediateZoom = ZoomController.update(true, 0.25D, false);
        check(Math.abs(immediateZoom - 0.25D) < 0.000001D,
                "non-smooth zoom must apply the configured factor immediately");

        double immediateReset = ZoomController.update(false, 0.25D, false);
        check(Math.abs(immediateReset - 1.0D) < 0.000001D,
                "releasing zoom without smoothing must restore normal FOV");

        ZoomController.reset();
        double smoothStart = ZoomController.update(true, 0.25D, true);
        check(smoothStart >= 0.25D && smoothStart <= 1.0D,
                "smooth zoom-in must remain between target and normal FOV");
        double smoothRelease = ZoomController.update(false, 0.25D, true);
        check(Double.isFinite(smoothRelease) && smoothRelease >= 0.25D && smoothRelease <= 1.0D,
                "smooth zoom-out must remain finite and within valid bounds");

        ZoomController.reset();
        System.out.println("ZoomControllerSelfTest: all checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
