package dev.opticore.client.shader;

/** Immutable, read-only weather, fog-colour, and camera light sample for one rendered frame. */
public final class SceneEnvironmentSnapshot {
    private final float fogRed, fogGreen, fogBlue;
    private final float rainStrength, thunderStrength, skyDarken;
    private final int blockLight, skyLight;

    public SceneEnvironmentSnapshot(float fogRed, float fogGreen, float fogBlue,
            float rainStrength, float thunderStrength, float skyDarken,
            int blockLight, int skyLight) {
        this.fogRed = finiteUnit(fogRed, "fogRed");
        this.fogGreen = finiteUnit(fogGreen, "fogGreen");
        this.fogBlue = finiteUnit(fogBlue, "fogBlue");
        this.rainStrength = finiteUnit(rainStrength, "rainStrength");
        this.thunderStrength = finiteUnit(thunderStrength, "thunderStrength");
        this.skyDarken = finiteUnit(skyDarken, "skyDarken");
        if (blockLight < 0 || blockLight > 15 || skyLight < 0 || skyLight > 15) {
            throw new IllegalArgumentException("Light levels must be in [0, 15]");
        }
        this.blockLight = blockLight;
        this.skyLight = skyLight;
    }

    public float fogRed() { return fogRed; }
    public float fogGreen() { return fogGreen; }
    public float fogBlue() { return fogBlue; }
    public float rainStrength() { return rainStrength; }
    public float thunderStrength() { return thunderStrength; }
    public float skyDarken() { return skyDarken; }
    public int blockLight() { return blockLight; }
    public int skyLight() { return skyLight; }

    private static float finiteUnit(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]");
        }
        return value;
    }
}
