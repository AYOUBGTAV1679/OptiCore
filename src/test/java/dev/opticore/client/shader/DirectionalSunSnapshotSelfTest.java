package dev.opticore.client.shader;

/** Regression checks for captured vanilla celestial state. */
public final class DirectionalSunSnapshotSelfTest {
    private DirectionalSunSnapshotSelfTest() {}

    public static void main(String[] args) {
        DirectionalSunSnapshot sample =
                new DirectionalSunSnapshot(1.25f, 0.5f, 1200L, 600L, 0.2f, 0.1f);
        check(sample.sunAngleRadians() == 1.25f, "sun angle retained");
        check(sample.partialTick() == 0.5f, "partial tick retained");
        check(sample.gameTime() == 1200L && sample.dayTime() == 600L, "world time retained");
        expectFailure(() -> new DirectionalSunSnapshot(Float.NaN, 0, 0, 0, 0, 0),
                "non-finite sun angle");
        expectFailure(() -> new DirectionalSunSnapshot(0, 1.1f, 0, 0, 0, 0),
                "partial tick outside range");
        expectFailure(() -> new DirectionalSunSnapshot(0, 0, 0, 0, 1.1f, 0),
                "rain outside range");
        System.out.println("DirectionalSunSnapshotSelfTest: OK");
    }

    private static void expectFailure(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError("Expected failure: " + label);
        } catch (IllegalArgumentException expected) {
            // Expected validation.
        }
    }
}
