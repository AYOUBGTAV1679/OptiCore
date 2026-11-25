package dev.opticore.client.shader;

/** Small dependency-free column-major 4x4 matrix inversion for captured scene matrices. */
public final class Matrix4Inverse {
    private Matrix4Inverse() {}

    /**
     * Returns the inverse of a column-major 4x4 matrix.
     * @throws IllegalArgumentException for malformed, non-finite, or singular matrices
     */
    public static float[] invert(float[] source) {
        if (source == null || source.length != 16) {
            throw new IllegalArgumentException("A 4x4 matrix must contain 16 values");
        }
        double[][] augmented = new double[4][8];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                float value = source[column * 4 + row];
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException("Matrix contains a non-finite value");
                }
                augmented[row][column] = value;
            }
            augmented[row][row + 4] = 1.0d;
        }

        for (int pivot = 0; pivot < 4; pivot++) {
            int bestRow = pivot;
            for (int row = pivot + 1; row < 4; row++) {
                if (Math.abs(augmented[row][pivot]) > Math.abs(augmented[bestRow][pivot])) {
                    bestRow = row;
                }
            }
            double pivotValue = augmented[bestRow][pivot];
            if (!Double.isFinite(pivotValue) || Math.abs(pivotValue) < 1.0e-12d) {
                throw new IllegalArgumentException("Matrix is singular and cannot be inverted");
            }
            double[] swap = augmented[pivot];
            augmented[pivot] = augmented[bestRow];
            augmented[bestRow] = swap;

            double divisor = augmented[pivot][pivot];
            for (int column = 0; column < 8; column++) {
                augmented[pivot][column] /= divisor;
            }
            for (int row = 0; row < 4; row++) {
                if (row == pivot) continue;
                double factor = augmented[row][pivot];
                for (int column = 0; column < 8; column++) {
                    augmented[row][column] -= factor * augmented[pivot][column];
                }
            }
        }

        float[] inverse = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                double value = augmented[row][column + 4];
                if (!Double.isFinite(value) || Math.abs(value) > Float.MAX_VALUE) {
                    throw new IllegalArgumentException("Matrix inverse is outside float range");
                }
                inverse[column * 4 + row] = (float) value;
            }
        }
        return inverse;
    }
}
