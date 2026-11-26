package dev.opticore.client.shader;

public final class AtmosphericFogColorSelfTest {
    private AtmosphericFogColorSelfTest() {}

    public static void main(String[] args) {
        float[] clear = AtmosphericFogColor.fromSky(0.2f, 0.4f, 0.8f, 0.0f, 0.0f);
        check(close(clear[0], 0.2f) && close(clear[1], 0.4f) && close(clear[2], 0.8f),
                "clear weather preserves biome sky color");

        float[] rainy = AtmosphericFogColor.fromSky(0.2f, 0.4f, 0.8f, 1.0f, 0.0f);
        check(close(rainy[0], 0.56f) && close(rainy[1], 0.59f) && close(rainy[2], 0.63f),
                "full rain blends toward overcast fog color");

        float[] thunder = AtmosphericFogColor.fromSky(0.2f, 0.4f, 0.8f, 0.0f, 1.0f);
        check(close(thunder[0], 0.524f) && close(thunder[1], 0.571f)
                        && close(thunder[2], 0.647f),
                "thunder applies the stronger storm blend");

        float[] mixed = AtmosphericFogColor.fromSky(0.2f, 0.4f, 0.8f, 0.5f, 0.2f);
        check(close(mixed[0], 0.38f) && close(mixed[1], 0.495f)
                        && close(mixed[2], 0.715f),
                "rain strength dominates weaker thunder");

        expectInvalid(Float.NaN, 0.0f, 0.0f, 0.0f, 0.0f, "non-finite sky color rejected");
        expectInvalid(0.0f, -0.1f, 0.0f, 0.0f, 0.0f, "negative sky color rejected");
        expectInvalid(0.0f, 0.0f, 1.1f, 0.0f, 0.0f, "out-of-range sky color rejected");
        expectInvalid(0.0f, 0.0f, 0.0f, Float.NaN, 0.0f, "non-finite rain rejected");
        expectInvalid(0.0f, 0.0f, 0.0f, 0.0f, -0.1f, "negative thunder rejected");
        System.out.println("AtmosphericFogColorSelfTest: 9 checks passed");
    }

    private static void expectInvalid(float r, float g, float b, float rain, float thunder, String label) {
        try {
            AtmosphericFogColor.fromSky(r, g, b, rain, thunder);
            throw new AssertionError(label);
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }

    private static boolean close(float left, float right) {
        return Math.abs(left - right) < 0.0001f;
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
