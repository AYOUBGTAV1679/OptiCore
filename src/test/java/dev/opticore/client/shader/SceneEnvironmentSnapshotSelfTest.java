package dev.opticore.client.shader;

/** Standalone validation checks for scene environment samples. */
public final class SceneEnvironmentSnapshotSelfTest {
    private SceneEnvironmentSnapshotSelfTest() {}

    public static void main(String[] args) {
        SceneEnvironmentSnapshot sample = new SceneEnvironmentSnapshot(
                0.2f, 0.4f, 0.8f, 0.5f, 0.25f, 0.1f, 15, 12);
        check(sample.fogGreen() == 0.4f, "fog channels");
        check(sample.rainStrength() == 0.5f && sample.thunderStrength() == 0.25f,
                "weather strengths");
        check(sample.blockLight() == 15 && sample.skyLight() == 12, "light levels");
        expectFailure(() -> new SceneEnvironmentSnapshot(1.1f, 0, 0, 0, 0, 0, 0, 0));
        expectFailure(() -> new SceneEnvironmentSnapshot(0, 0, 0, -0.1f, 0, 0, 0, 0));
        expectFailure(() -> new SceneEnvironmentSnapshot(0, 0, 0, 0, 0, 0, 16, 0));
        System.out.println("SceneEnvironmentSnapshotSelfTest: OK");
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected invalid environment sample to be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
