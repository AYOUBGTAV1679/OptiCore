package dev.opticore.client.shader;

/** Builds a bounded sky-tinted fog color from the live biome sky and weather state. */
public final class AtmosphericFogColor {
    private static final float OVERCAST_RED = 0.56f;
    private static final float OVERCAST_GREEN = 0.59f;
    private static final float OVERCAST_BLUE = 0.63f;

    private AtmosphericFogColor() {}

    /**
     * Blends the current sky color toward a muted overcast tone as rain and
     * thunder increase. All inputs and outputs are normalized to [0, 1].
     */
    public static float[] fromSky(float red, float green, float blue,
                                  float rainStrength, float thunderStrength) {
        requireUnit(red, "red");
        requireUnit(green, "green");
        requireUnit(blue, "blue");
        requireUnit(rainStrength, "rainStrength");
        requireUnit(thunderStrength, "thunderStrength");
        float overcast = Math.max(rainStrength, thunderStrength * 0.9f);
        return new float[]{
                mix(red, OVERCAST_RED, overcast),
                mix(green, OVERCAST_GREEN, overcast),
                mix(blue, OVERCAST_BLUE, overcast)
        };
    }

    private static float mix(float from, float to, float amount) {
        return from + (to - from) * amount;
    }

    private static void requireUnit(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]");
        }
    }
}
