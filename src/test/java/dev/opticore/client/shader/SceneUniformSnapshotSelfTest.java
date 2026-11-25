package dev.opticore.client.shader;

/** Standalone regression checks for the immutable scene-uniform snapshot. */
public final class SceneUniformSnapshotSelfTest {
    private SceneUniformSnapshotSelfTest() {}

    public static void main(String[] args) {
        float[] modelView = identity();
        float[] projection = identity();
        SceneUniformSnapshot snapshot = new SceneUniformSnapshot(modelView, projection,
                12.5, 64.0, -3.25, 0.5f, 100, 1920, 1080,
                2000L, 26001L, "minecraft:overworld");

        modelView[0] = 99.0f;
        projection[5] = 99.0f;
        check(snapshot.modelViewMatrix()[0] == 1.0f, "constructor must copy matrices");
        float[] returned = snapshot.projectionMatrix();
        returned[5] = 77.0f;
        check(snapshot.projectionMatrix()[5] == 1.0f, "accessor must return a copy");
        check(Math.abs(snapshot.timeSeconds() - 100.025d) < 0.000001d, "continuous time");
        check(snapshot.normalizedDayTime() == 2001L, "normalized day time");
        check(snapshot.viewportWidth() == 1920 && snapshot.viewportHeight() == 1080,
                "viewport dimensions");
        check(snapshot.dimension().equals("minecraft:overworld"), "dimension identifier");
        check(snapshot.modelViewMatrixInverse()[0] == 1.0f,
                "identity model-view inverse");
        check(snapshot.projectionMatrixInverse()[15] == 1.0f,
                "identity projection inverse");
        check(snapshot.normalMatrix()[0] == 1.0f && snapshot.normalMatrix()[4] == 1.0f
                        && snapshot.normalMatrix()[8] == 1.0f,
                "identity normal matrix");

        float[] translated = identity();
        translated[12] = 4.0f;
        translated[13] = -2.0f;
        translated[14] = 7.0f;
        SceneUniformSnapshot translatedSnapshot = new SceneUniformSnapshot(translated, identity(),
                0, 0, 0, 0, 0, 1, 1, 0, -1, "minecraft:overworld");
        float[] inverse = translatedSnapshot.modelViewMatrixInverse();
        check(inverse[12] == -4.0f && inverse[13] == 2.0f && inverse[14] == -7.0f,
                "inverse must undo camera translation");
        check(translatedSnapshot.normalizedDayTime() == 23999L,
                "negative day time must wrap into the Minecraft day range");

        expectFailure(() -> new SceneUniformSnapshot(new float[15], identity(),
                0, 0, 0, 0, 0, 1, 1, 0, 0, "minecraft:overworld"),
                "reject malformed matrices");
        float[] invalid = identity();
        invalid[3] = Float.NaN;
        expectFailure(() -> new SceneUniformSnapshot(invalid, identity(),
                0, 0, 0, 0, 0, 1, 1, 0, 0, "minecraft:overworld"),
                "reject non-finite matrices");
        float[] singular = identity();
        singular[0] = 0.0f;
        SceneUniformSnapshot singularSnapshot = new SceneUniformSnapshot(singular, identity(),
                0, 0, 0, 0, 0, 1, 1, 0, 0, "minecraft:overworld");
        expectFailure(singularSnapshot::modelViewMatrixInverse,
                "reject singular model-view inverse");
        expectFailure(() -> new SceneUniformSnapshot(identity(), identity(),
                0, 0, 0, 1.1f, 0, 1, 1, 0, 0, "minecraft:overworld"),
                "reject invalid partial tick");
        System.out.println("SceneUniformSnapshotSelfTest: OK");
    }

    private static float[] identity() {
        return new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
