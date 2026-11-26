package dev.opticore.client.shader;

/**
 * Validated view-space distance interval for depth-based atmospheric fog.
 * Invalid persisted values fall back safely instead of producing undefined GLSL smoothstep input.
 */
public record AtmosphericFogRange(float start, float end) {
    private static final float DEFAULT_START = 48.0f;
    private static final float DEFAULT_END = 192.0f;
    private static final float MAX_START = 2048.0f;
    private static final float MAX_END = 4096.0f;

    public AtmosphericFogRange {
        if (!Float.isFinite(start) || !Float.isFinite(end)
                || start < 0.0f || end <= start) {
            throw new IllegalArgumentException("Fog end must be finite and greater than fog start");
        }
    }

    public static AtmosphericFogRange fromConfig(double configuredStart, double configuredEnd) {
        float start = Double.isFinite(configuredStart)
                ? (float) Math.max(0.0, Math.min(MAX_START, configuredStart))
                : DEFAULT_START;
        float end = Double.isFinite(configuredEnd)
                ? (float) Math.max(2.0, Math.min(MAX_END, configuredEnd))
                : DEFAULT_END;
        if (end <= start) end = Math.min(MAX_END, start + 1.0f);
        return new AtmosphericFogRange(start, end);
    }
}
