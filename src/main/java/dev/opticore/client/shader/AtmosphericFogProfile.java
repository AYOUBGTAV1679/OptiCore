package dev.opticore.client.shader;

/** Weather-responsive depth-fog controls for the fullscreen world composite. */
public record AtmosphericFogProfile(float strength, float start, float end) {
    public AtmosphericFogProfile {
        if (!Float.isFinite(strength) || strength < 0.0f || strength > 1.0f
                || !Float.isFinite(start) || !Float.isFinite(end)
                || start < 0.0f || end <= start) {
            throw new IllegalArgumentException("Invalid atmospheric fog profile");
        }
    }

    public static AtmosphericFogProfile forWeather(float baseStrength, AtmosphericFogRange range,
                                                    float rain, float thunder) {
        if (!Float.isFinite(baseStrength) || baseStrength < 0.0f || baseStrength > 1.0f
                || !Float.isFinite(rain) || rain < 0.0f || rain > 1.0f
                || !Float.isFinite(thunder) || thunder < 0.0f || thunder > 1.0f) {
            throw new IllegalArgumentException("Fog strength and weather must be finite and in [0, 1]");
        }
        if (range == null) throw new IllegalArgumentException("Fog range is required");
        float overcast = Math.max(rain, thunder * 0.9f);
        float strength = Math.min(1.0f, baseStrength * (1.0f + 0.65f * overcast));
        float start = range.start() * (1.0f - 0.25f * overcast);
        float end = Math.max(start + 1.0f, range.end() * (1.0f - 0.20f * overcast));
        return new AtmosphericFogProfile(strength, start, end);
    }
}
