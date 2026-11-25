package dev.opticore.client.shader;

import java.nio.FloatBuffer;

/** Regression checks for finite GLSL uniform input validation. */
public final class ShaderUniformValidatorSelfTest {
    private static int checks;
    private ShaderUniformValidatorSelfTest() {}

    public static void main(String[] args) {
        acceptsFiniteScalar();
        rejectsNonFiniteScalars();
        acceptsFiniteMatrixWithoutMovingPosition();
        rejectsNonFiniteMatrixWithoutMovingPosition();
        rejectsShortMatrix();
        rejectsInvalidMatrixCounts();
        ignoresValuesOutsideValidatedRange();
        System.out.println("ShaderUniformValidatorSelfTest: " + checks + " checks passed");
    }

    private static void acceptsFiniteScalar() {
        ShaderUniformValidator.requireFinite(0.5f, "uValue");
        checks++;
    }

    private static void rejectsNonFiniteScalars() {
        expectFailure(() -> ShaderUniformValidator.requireFinite(Float.NaN, "uValue"));
        expectFailure(() -> ShaderUniformValidator.requireFinite(Float.POSITIVE_INFINITY, "uValue"));
        expectFailure(() -> ShaderUniformValidator.requireFinite(Float.NEGATIVE_INFINITY, "uValue"));
    }

    private static void acceptsFiniteMatrixWithoutMovingPosition() {
        FloatBuffer values = FloatBuffer.wrap(new float[] {9, 1, 2, 3, 4});
        values.position(1);
        ShaderUniformValidator.requireFinite(values, 4, "uMatrix");
        if (values.position() != 1) throw new AssertionError("Matrix validation changed buffer position");
        checks++;
    }

    private static void rejectsNonFiniteMatrixWithoutMovingPosition() {
        FloatBuffer values = FloatBuffer.wrap(new float[] {9, 1, Float.NaN, 3});
        values.position(1);
        expectFailure(() -> ShaderUniformValidator.requireFinite(values, 3, "uMatrix"));
        if (values.position() != 1) throw new AssertionError("Failed validation changed buffer position");
    }

    private static void rejectsShortMatrix() {
        expectFailure(() -> ShaderUniformValidator.requireFinite(FloatBuffer.wrap(new float[] {1, 2}), 3, "uMatrix"));
    }

    private static void rejectsInvalidMatrixCounts() {
        expectFailure(() -> ShaderUniformValidator.requireFinite(FloatBuffer.wrap(new float[] {1}), 0, "uMatrix"));
        expectFailure(() -> ShaderUniformValidator.requireFinite(FloatBuffer.wrap(new float[] {1}), -1, "uMatrix"));
    }

    private static void ignoresValuesOutsideValidatedRange() {
        FloatBuffer values = FloatBuffer.wrap(new float[] {1, Float.NaN});
        ShaderUniformValidator.requireFinite(values, 1, "uMatrix");
        checks++;
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected invalid uniform data to be rejected");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }
}
