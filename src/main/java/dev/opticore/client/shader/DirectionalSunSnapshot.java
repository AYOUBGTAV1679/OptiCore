package dev.opticore.client.shader;

/**
 * Immutable sample of Minecraft's interpolated celestial sun angle.
 *
 * <p>The angle is the value returned by Level#getSunAngle(partialTick), in
 * radians. It deliberately does not claim to be a world-space light vector:
 * converting the vanilla sky transform into a directional-light basis is a
 * separate renderer integration step.</p>
 */
public record DirectionalSunSnapshot(float sunAngleRadians, float partialTick,
                                     long gameTime, long dayTime,
                                     float rain, float thunder) {
    /** Sine of the captured celestial angle; this is not a world-space light vector. */
    public float sunAngleSin() {
        return (float) Math.sin(sunAngleRadians);
    }

    /** Cosine of the captured celestial angle; this is not a world-space light vector. */
    public float sunAngleCos() {
        return (float) Math.cos(sunAngleRadians);
    }

    public DirectionalSunSnapshot {
        if (!Float.isFinite(sunAngleRadians) || !Float.isFinite(partialTick)
                || !Float.isFinite(rain) || !Float.isFinite(thunder)
                || partialTick < 0.0f || partialTick > 1.0f
                || rain < 0.0f || rain > 1.0f
                || thunder < 0.0f || thunder > 1.0f) {
            throw new IllegalArgumentException("Invalid directional sun snapshot");
        }
    }
}
