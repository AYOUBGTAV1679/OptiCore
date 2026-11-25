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
        float[] verticalView = vertical.viewMatrix();
        float[] verticalProjection = vertical.projectionMatrix();
        float centerViewZ = verticalView[2] * 12.0f + verticalView[6] * 80.0f
                + verticalView[10] * -4.0f + verticalView[14];
        float centerClipZ = verticalProjection[10] * centerViewZ + verticalProjection[14];
        check(close(centerClipZ, 0.0f),
                "shadow focus point must map to the center of the depth interval");
        float nearClipZ = projectedClipZ(verticalView, verticalProjection,
                12.0f, 119.5f, -4.0f);
        float farClipZ = projectedClipZ(verticalView, verticalProjection,
                12.0f, 40.5f, -4.0f);
        check(close(nearClipZ, -1.0f), "near depth bound maps to OpenGL clip -1");
        check(close(farClipZ, 1.0f), "far depth bound maps to OpenGL clip +1");

        // Exercise every cardinal light axis, including both vertical singularity cases.
        double[][] cardinalLights = {
                {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}
        };
        for (double[] light : cardinalLights) {
            DirectionalShadowCamera.Camera camera = DirectionalShadowCamera.create(
                    12, 80, -4, light[0], light[1], light[2], 16, 1, 80, 256);
            float[] matrix = camera.viewProjectionMatrix();
            checkFinite(matrix, "cardinal-light view-projection");
            float[] focusClip = projectClip(matrix,
                    camera.centerX(), camera.centerY(), camera.centerZ());
            check(close(focusClip[0], 0) && close(focusClip[1], 0) && close(focusClip[2], 0),
                    "snapped focus must remain at clip-space center for cardinal light "
                            + light[0] + "," + light[1] + "," + light[2]);
        }

        float[] copy = first.viewMatrix();
        copy[0] = 999;
        check(!close(first.viewMatrix()[0], 999), "view matrix accessor is defensive");
        float[] projectionCopy = first.projectionMatrix();
        projectionCopy[0] = 999;
        check(!close(first.projectionMatrix()[0], 999),
                "projection matrix accessor is defensive");
        float[] viewProjectionCopy = first.viewProjectionMatrix();
        viewProjectionCopy[0] = 999;
        check(!close(first.viewProjectionMatrix()[0], 999),
                "view-projection matrix accessor is defensive");

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

    private static float[] projectClip(float[] matrix, double x, double y, double z) {
        return new float[] {
                (float)(matrix[0]*x + matrix[4]*y + matrix[8]*z + matrix[12]),
                (float)(matrix[1]*x + matrix[5]*y + matrix[9]*z + matrix[13]),
                (float)(matrix[2]*x + matrix[6]*y + matrix[10]*z + matrix[14])
        };
    }
    private static float projectedClipZ(float[] view, float[] projection,
                                         float x, float y, float z) {
        float viewZ = view[2] * x + view[6] * y + view[10] * z + view[14];
        return projection[10] * viewZ + projection[14];
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
