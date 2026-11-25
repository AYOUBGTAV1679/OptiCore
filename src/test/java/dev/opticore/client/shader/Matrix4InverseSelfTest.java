package dev.opticore.client.shader;

/** Standalone regression checks for column-major 4x4 matrix inversion. */
public final class Matrix4InverseSelfTest {
    private Matrix4InverseSelfTest() {}

    public static void main(String[] args) {
        float[] identity = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        checkMatrix(identity, Matrix4Inverse.invert(identity), "identity inverse");

        float[] translation = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 5, -2, 7, 1};
        float[] inverse = Matrix4Inverse.invert(translation);
        check(close(inverse[12], -5) && close(inverse[13], 2) && close(inverse[14], -7),
                "translation inverse");

        float[] product = multiply(translation, inverse);
        checkMatrix(identity, product, "matrix multiplied by inverse");
        expectFailure(() -> Matrix4Inverse.invert(new float[16]), "singular matrix");
        expectFailure(() -> Matrix4Inverse.invert(new float[15]), "wrong matrix size");
        System.out.println("Matrix4InverseSelfTest: OK");
    }

    private static float[] multiply(float[] left, float[] right) {
        float[] result = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                for (int k = 0; k < 4; k++) {
                    result[column * 4 + row] += left[k * 4 + row] * right[column * 4 + k];
                }
            }
        }
        return result;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void checkMatrix(float[] expected, float[] actual, String message) {
        for (int i = 0; i < 16; i++) {
            if (!close(expected[i], actual[i])) throw new AssertionError(message + " at " + i);
        }
    }

    private static boolean close(float a, float b) { return Math.abs(a - b) < 0.0001f; }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError("Expected failure: " + message);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
