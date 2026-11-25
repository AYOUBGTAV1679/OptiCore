package dev.opticore.client.shader;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, graphics-API-independent scene data captured at a Forge render stage.
 * Matrix arrays use JOML's column-major layout and are defensively copied.
 */
public final class SceneUniformSnapshot {
    private final float[] modelViewMatrix;
    private final float[] projectionMatrix;
    private final double cameraX, cameraY, cameraZ;
    private final float partialTick;
    private final int renderTick, viewportWidth, viewportHeight;
    private final long gameTime, dayTime;
    private final String dimension;

    public SceneUniformSnapshot(float[] modelViewMatrix, float[] projectionMatrix,
            double cameraX, double cameraY, double cameraZ, float partialTick,
            int renderTick, int viewportWidth, int viewportHeight,
            long gameTime, long dayTime, String dimension) {
        this.modelViewMatrix = copyMatrix(modelViewMatrix, "modelViewMatrix");
        this.projectionMatrix = copyMatrix(projectionMatrix, "projectionMatrix");
        requireFinite(cameraX, "cameraX");
        requireFinite(cameraY, "cameraY");
        requireFinite(cameraZ, "cameraZ");
        if (!Float.isFinite(partialTick) || partialTick < 0.0f || partialTick > 1.0f) {
            throw new IllegalArgumentException("partialTick must be finite and between 0 and 1");
        }
        if (viewportWidth < 0 || viewportHeight < 0) {
            throw new IllegalArgumentException("Viewport dimensions cannot be negative");
        }
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.partialTick = partialTick;
        this.renderTick = renderTick;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.gameTime = gameTime;
        this.dayTime = dayTime;
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        if (dimension.isBlank() || dimension.length() > 256) {
            throw new IllegalArgumentException("Invalid dimension identifier");
        }
    }

    public float[] modelViewMatrix() { return modelViewMatrix.clone(); }
    public float[] projectionMatrix() { return projectionMatrix.clone(); }
    public float[] modelViewMatrixInverse() { return Matrix4Inverse.invert(modelViewMatrix); }
    public float[] projectionMatrixInverse() { return Matrix4Inverse.invert(projectionMatrix); }
    public float[] normalMatrix() { return Matrix3Normal.fromModelView(modelViewMatrix); }
    public double cameraX() { return cameraX; }
    public double cameraY() { return cameraY; }
    public double cameraZ() { return cameraZ; }
    public float partialTick() { return partialTick; }
    public int renderTick() { return renderTick; }
    public int viewportWidth() { return viewportWidth; }
    public int viewportHeight() { return viewportHeight; }
    public long gameTime() { return gameTime; }
    public long dayTime() { return dayTime; }
    public String dimension() { return dimension; }

    /** Continuous world time in seconds, derived from game ticks and render partial tick. */
    public double timeSeconds() {
        return (gameTime + partialTick) / 20.0d;
    }

    /** Day-cycle time normalized to [0, 24000). */
    public long normalizedDayTime() {
        return Math.floorMod(dayTime, 24000L);
    }

    private static float[] copyMatrix(float[] matrix, String name) {
        Objects.requireNonNull(matrix, name);
        if (matrix.length != 16) throw new IllegalArgumentException(name + " must contain 16 values");
        float[] copy = matrix.clone();
        for (float value : copy) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " contains a non-finite value");
        }
        return copy;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }

    @Override
    public String toString() {
        return "SceneUniformSnapshot{dimension='" + dimension + "', tick=" + renderTick
                + ", viewport=" + viewportWidth + "x" + viewportHeight
                + ", camera=(" + cameraX + ", " + cameraY + ", " + cameraZ + ")}";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SceneUniformSnapshot that)) return false;
        return Arrays.equals(modelViewMatrix, that.modelViewMatrix)
                && Arrays.equals(projectionMatrix, that.projectionMatrix)
                && Double.compare(cameraX, that.cameraX) == 0
                && Double.compare(cameraY, that.cameraY) == 0
                && Double.compare(cameraZ, that.cameraZ) == 0
                && Float.compare(partialTick, that.partialTick) == 0
                && renderTick == that.renderTick && viewportWidth == that.viewportWidth
                && viewportHeight == that.viewportHeight && gameTime == that.gameTime
                && dayTime == that.dayTime && dimension.equals(that.dimension);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(modelViewMatrix);
        result = 31 * result + Arrays.hashCode(projectionMatrix);
        result = 31 * result + Objects.hash(cameraX, cameraY, cameraZ, partialTick,
                renderTick, viewportWidth, viewportHeight, gameTime, dayTime, dimension);
        return result;
    }
}
