package dev.opticore.client.shader;

public final class AtmosphericFogProfileSelfTest {
    private AtmosphericFogProfileSelfTest() {}

    public static void main(String[] args) {
        AtmosphericFogRange range = new AtmosphericFogRange(48.0f, 192.0f);
        AtmosphericFogProfile clear = AtmosphericFogProfile.forWeather(0.4f, range, 0.0f, 0.0f);
        check(close(clear.strength(), 0.4f) && close(clear.start(), 48.0f)
                && close(clear.end(), 192.0f), "clear weather preserves configured fog");

        AtmosphericFogProfile rain = AtmosphericFogProfile.forWeather(0.4f, range, 1.0f, 0.0f);
        check(close(rain.strength(), 0.66f) && close(rain.start(), 36.0f)
                && close(rain.end(), 153.6f), "heavy rain increases fog and moves its range closer");

        AtmosphericFogProfile thunder = AtmosphericFogProfile.forWeather(0.4f, range, 0.0f, 1.0f);
        check(close(thunder.strength(), 0.634f) && close(thunder.start(), 37.2f),
                "thunder contributes to storm fog strength");

        AtmosphericFogProfile capped = AtmosphericFogProfile.forWeather(1.0f, range, 1.0f, 1.0f);
        check(capped.strength() == 1.0f, "weather-adjusted fog strength is capped");

        expectInvalid(Float.NaN, range, 0.0f, 0.0f, "non-finite base strength rejected");
        expectInvalid(0.4f, null, 0.0f, 0.0f, "missing fog range rejected");
        expectInvalid(0.4f, range, 1.1f, 0.0f, "out-of-range rain rejected");
        System.out.println("AtmosphericFogProfileSelfTest: 7 checks passed");
    }

    private static void expectInvalid(float strength, AtmosphericFogRange range,
                                      float rain, float thunder, String label) {
        try {
            AtmosphericFogProfile.forWeather(strength, range, rain, thunder);
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
