package dev.opticore.client.shader;

/** Computes the inverse-transpose 3x3 normal matrix from a column-major model-view matrix. */
public final class Matrix3Normal {
    private Matrix3Normal() {}

    /**
     * Returns a column-major mat3 suitable for transforming surface normals.
     * Translation is ignored; singular or non-finite linear transforms are rejected.
     */
    public static float[] fromModelView(float[] modelView) {
        if (modelView == null || modelView.length != 16) {
            throw new IllegalArgumentException("A model-view matrix must contain 16 values");
        }
        double a00 = modelView[0], a01 = modelView[4], a02 = modelView[8];
        double a10 = modelView[1], a11 = modelView[5], a12 = modelView[9];
        double a20 = modelView[2], a21 = modelView[6], a22 = modelView[10];
        double[] values = {a00,a01,a02,a10,a11,a12,a20,a21,a22};
        for (double value : values) {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Model-view matrix is non-finite");
        }

        double c00 = a11 * a22 - a12 * a21;
        double c01 = a12 * a20 - a10 * a22;
        double c02 = a10 * a21 - a11 * a20;
        double determinant = a00 * c00 + a01 * c01 + a02 * c02;
        if (!Double.isFinite(determinant) || Math.abs(determinant) < 1.0e-12d) {
            throw new IllegalArgumentException("Model-view linear transform is singular");
        }

        // Inverse, in row-major order.
        double[] inverse = {
                c00, a02 * a21 - a01 * a22, a01 * a12 - a02 * a11,
                c01, a00 * a22 - a02 * a20, a02 * a10 - a00 * a12,
                c02, a01 * a20 - a00 * a21, a00 * a11 - a01 * a10
        };
        float[] normal = new float[9];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                double value = inverse[column * 3 + row] / determinant;
                if (!Double.isFinite(value) || Math.abs(value) > Float.MAX_VALUE) {
                    throw new IllegalArgumentException("Normal matrix is outside float range");
                }
                normal[column * 3 + row] = (float) value;
            }
        }
        return normal;
    }
}
