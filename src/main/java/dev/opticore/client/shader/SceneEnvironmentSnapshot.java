package dev.opticore.client.shader;

/** Immutable, read-only weather, sky-colour, and camera light sample for one rendered frame. */
public final class SceneEnvironmentSnapshot {
    private final float skyRed, skyGreen, skyBlue;
    private final float rainStrength, thunderStrength, skyDarken;
    private final int blockLight, skyLight;

    public SceneEnvironmentSnapshot(float skyRed, float skyGreen, float skyBlue,
            float rainStrength, float thunderStrength, float skyDarken,
            int blockLight, int skyLight) {
        this.skyRed = finiteUnit(skyRed, "skyRed");
        this.skyGreen = finiteUnit(skyGreen, "skyGreen");
        this.skyBlue = finiteUnit(skyBlue, "skyBlue");
        this.rainStrength = finiteUnit(rainStrength, "rainStrength");
        this.thunderStrength = finiteUnit(thunderStrength, "thunderStrength");
        this.skyDarken = finiteUnit(skyDarken, "skyDarken");
        if (blockLight < 0 || blockLight > 15 || skyLight < 0 || skyLight > 15) {
            throw new IllegalArgumentException("Light levels must be in [0, 15]");
        }
        this.blockLight = blockLight;
        this.skyLight = skyLight;
    }

    public float skyRed() { return skyRed; }
    public float skyGreen() { return skyGreen; }
    public float skyBlue() { return skyBlue; }
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
