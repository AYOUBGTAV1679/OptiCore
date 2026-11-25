package dev.opticore.client.shader;

/** Regression checks for stable directional shadow camera construction. */
public final class DirectionalShadowCameraSelfTest {
    private DirectionalShadowCameraSelfTest() {}

    public static void main(String[] args) {
        DirectionalShadowCamera.Camera first =
                DirectionalShadowCamera.create(0.01, 64, 0.02, 0.3, 1, -0.2, 8, 1, 40, 64);
        DirectionalShadowCamera.Camera subTexel =
                DirectionalShadowCamera.create(0.02, 64, 0.03, 0.3, 1, -0.2, 8, 1, 40, 64);
        checkMatrix(first.viewProjectionMatrix(), subTexel.viewProjectionMatrix(),
                "sub-texel movement should not move the shadow camera");

        DirectionalShadowCamera.Camera nextTexel =
                DirectionalShadowCamera.create(0.30, 64, 0.02, 0.3, 1, -0.2, 8, 1, 40, 64);
        check(!same(first.viewProjectionMatrix(), nextTexel.viewProjectionMatrix()),
                "crossing a texel boundary should move the shadow camera");

        DirectionalShadowCamera.Camera vertical =
                DirectionalShadowCamera.create(12, 80, -4, 0, 1, 0, 16, 1, 80, 256);
        checkFinite(vertical.viewMatrix(), "vertical-light view");
        checkFinite(vertical.projectionMatrix(), "vertical-light projection");
        check(close((float) vertical.worldUnitsPerTexel(), 0.125f), "world-units-per-texel");

        float[] copy = first.viewMatrix();
        copy[0] = 999;
        check(!close(first.viewMatrix()[0], 999), "matrix accessor is defensive");

        expectFailure(() -> DirectionalShadowCamera.create(0,0,0,0,0,0,8,1,40,64),
                "zero light direction");
        expectFailure(() -> DirectionalShadowCamera.create(0,0,0,0,1,0,0,1,40,64),
                "zero extent");
        expectFailure(() -> DirectionalShadowCamera.create(0,0,0,0,1,0,8,1,1,64),
                "invalid depth range");
        expectFailure(() -> DirectionalShadowCamera.create(0,0,0,0,1,0,8,1,40,0),
                "zero resolution");
        expectFailure(() -> DirectionalShadowCamera.create(Double.MAX_VALUE,0,0,0,1,0,8,1,40,64),
                "camera position outside float matrix range");
        expectFailure(() -> DirectionalShadowCamera.create(0,0,0,Double.MAX_VALUE,Double.MAX_VALUE,
                Double.MAX_VALUE,8,1,40,64), "unrepresentable light direction");
        System.out.println("DirectionalShadowCameraSelfTest: OK");
    }

    private static boolean same(float[] a, float[] b) {
        for (int i = 0; i < 16; i++) if (!close(a[i], b[i])) return false;
        return true;
    }
    private static void checkMatrix(float[] a, float[] b, String message) {
        check(same(a,b), message);
    }
    private static void checkFinite(float[] values, String message) {
        for (float value : values) check(Float.isFinite(value), message);
    }
    private static boolean close(float a, float b) { return Math.abs(a-b) < 0.0001f; }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void expectFailure(Runnable action, String message) {
        try { action.run(); throw new AssertionError("Expected failure: " + message); }
        catch (IllegalArgumentException expected) { }
    }
}
