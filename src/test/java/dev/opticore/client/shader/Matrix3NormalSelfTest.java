package dev.opticore.client.shader;

/** Standalone checks for inverse-transpose normal matrix generation. */
public final class Matrix3NormalSelfTest {
    private Matrix3NormalSelfTest() {}

    public static void main(String[] args) {
        float[] identity = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
        checkMatrix(new float[]{1,0,0, 0,1,0, 0,0,1},
                Matrix3Normal.fromModelView(identity), "identity");

        float[] translatedScaled = {2,0,0,0, 0,4,0,0, 0,0,5,0, 9,-3,2,1};
        checkMatrix(new float[]{0.5f,0,0, 0,0.25f,0, 0,0,0.2f},
                Matrix3Normal.fromModelView(translatedScaled), "inverse scale and ignored translation");

        float[] shear = {1,0,0,0, 2,1,0,0, 0,0,1,0, 0,0,0,1};
        checkMatrix(new float[]{1,-2,0, 0,1,0, 0,0,1},
                Matrix3Normal.fromModelView(shear), "inverse transpose shear");
        expectFailure(() -> Matrix3Normal.fromModelView(new float[16]), "singular transform");
        expectFailure(() -> Matrix3Normal.fromModelView(new float[15]), "wrong matrix size");
        System.out.println("Matrix3NormalSelfTest: OK");
    }

    private static void checkMatrix(float[] expected, float[] actual, String message) {
        for (int i=0;i<9;i++) if (Math.abs(expected[i]-actual[i])>0.0001f)
            throw new AssertionError(message + " at " + i + ": " + actual[i]);
    }
    private static void expectFailure(Runnable action, String message) {
        try { action.run(); throw new AssertionError("Expected failure: " + message); }
        catch (IllegalArgumentException expected) { }
    }
}
