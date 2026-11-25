package dev.opticore.client.shader;

import java.nio.FloatBuffer;

/** Shared finite-value checks for values uploaded to GLSL uniforms. */
final class ShaderUniformValidator {
    private ShaderUniformValidator() {}

    static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Uniform '" + name + "' must be finite");
        }
    }

    static void requireFinite(FloatBuffer values, int count, String name) {
        if (values == null || count < 0 || values.remaining() < count) {
            throw new IllegalArgumentException("Uniform matrix '" + name + "' has insufficient values");
        }
        int start = values.position();
        for (int i = 0; i < count; i++) {
            if (!Float.isFinite(values.get(start + i))) {
                throw new IllegalArgumentException("Uniform matrix '" + name + "' contains a non-finite value");
            }
        }
    }
}
