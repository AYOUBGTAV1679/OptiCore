package dev.opticore.client.shader;

/**
 * Builds a stable orthographic camera for a directional-light shadow map.
 *
 * <p>The light direction points from the shaded scene toward the light.
 * The camera is centered on a world-space focus point and snapped to shadow
 * texels to reduce sub-texel shadow swimming. This class only creates matrices;
 * it does not infer Minecraft's sun direction or render any world geometry.</p>
 */
public final class DirectionalShadowCamera {
    private DirectionalShadowCamera() {}

    public record Camera(float[] viewMatrix, float[] projectionMatrix,
                         float[] viewProjectionMatrix, double centerX,
                         double centerY, double centerZ, double worldUnitsPerTexel) {
        public Camera {
            viewMatrix = viewMatrix.clone();
            projectionMatrix = projectionMatrix.clone();
            viewProjectionMatrix = viewProjectionMatrix.clone();
        }
        @Override public float[] viewMatrix() { return viewMatrix.clone(); }
        @Override public float[] projectionMatrix() { return projectionMatrix.clone(); }
        @Override public float[] viewProjectionMatrix() { return viewProjectionMatrix.clone(); }
    }

    /**
     * @param center world-space focus point, normally the camera position
     * @param lightDirection direction from the scene toward the directional light
     * @param halfExtent half-width/height of the square shadow coverage
     * @param near positive near plane distance
     * @param far far plane distance, greater than near
     * @param resolution shadow map width and height in texels
     */
    public static Camera create(double centerX, double centerY, double centerZ,
                                double lightX, double lightY, double lightZ,
                                double halfExtent, double near, double far, int resolution) {
        finite(centerX, centerY, centerZ, lightX, lightY, lightZ, halfExtent, near, far);
        if (!(halfExtent > 0.0) || !(near > 0.0) || !(far > near)
                || resolution < 1 || resolution > 8192) {
            throw new IllegalArgumentException("Invalid directional shadow camera bounds");
        }

        double lightLength = length(lightX, lightY, lightZ);
        if (lightLength < 1.0e-12) throw new IllegalArgumentException("Light direction is degenerate");
        lightX /= lightLength;
        lightY /= lightLength;
        lightZ /= lightLength;

        // Camera looks opposite the direction toward the light.
        double forwardX = -lightX, forwardY = -lightY, forwardZ = -lightZ;
        double upX = Math.abs(forwardY) > 0.95 ? 0.0 : 0.0;
        double upY = Math.abs(forwardY) > 0.95 ? 0.0 : 1.0;
        double upZ = Math.abs(forwardY) > 0.95 ? 1.0 : 0.0;

        double rightX = forwardY * upZ - forwardZ * upY;
        double rightY = forwardZ * upX - forwardX * upZ;
        double rightZ = forwardX * upY - forwardY * upX;
        double rightLength = length(rightX, rightY, rightZ);
        rightX /= rightLength; rightY /= rightLength; rightZ /= rightLength;

        double trueUpX = rightY * forwardZ - rightZ * forwardY;
        double trueUpY = rightZ * forwardX - rightX * forwardZ;
        double trueUpZ = rightX * forwardY - rightY * forwardX;

        double texel = (2.0 * halfExtent) / resolution;
        double rightCoordinate = dot(centerX, centerY, centerZ, rightX, rightY, rightZ);
        double upCoordinate = dot(centerX, centerY, centerZ, trueUpX, trueUpY, trueUpZ);
        double snappedRight = Math.floor(rightCoordinate / texel + 0.5) * texel;
        double snappedUp = Math.floor(upCoordinate / texel + 0.5) * texel;
        double shiftRight = snappedRight - rightCoordinate;
        double shiftUp = snappedUp - upCoordinate;

        double snappedX = centerX + rightX * shiftRight + trueUpX * shiftUp;
        double snappedY = centerY + rightY * shiftRight + trueUpY * shiftUp;
        double snappedZ = centerZ + rightZ * shiftRight + trueUpZ * shiftUp;

        // Keep the camera far enough along the light ray for the requested near plane.
        double distance = near + halfExtent;
        float[] view = ShadowCameraMatrices.lookAt(
                snappedX + lightX * distance, snappedY + lightY * distance, snappedZ + lightZ * distance,
                snappedX, snappedY, snappedZ, upX, upY, upZ);
        float[] projection = ShadowCameraMatrices.orthographic(
                -halfExtent, halfExtent, -halfExtent, halfExtent, near, far);
        return new Camera(view, projection,
                ShadowCameraMatrices.multiply(projection, view),
                snappedX, snappedY, snappedZ, texel);
    }

    private static double dot(double ax, double ay, double az, double bx, double by, double bz) {
        return ax * bx + ay * by + az * bz;
    }
    private static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
    private static void finite(double... values) {
        for (double value : values) if (!Double.isFinite(value))
            throw new IllegalArgumentException("Directional shadow camera values must be finite");
    }
}
