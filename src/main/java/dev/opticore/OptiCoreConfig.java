package dev.opticore;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OptiCoreConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_FPS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ZOOM;
    public static final ForgeConfigSpec.BooleanValue SHOW_CHUNK_DIAGNOSTICS;
    public static final ForgeConfigSpec.DoubleValue ZOOM_FACTOR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        SHOW_FPS = builder.comment("Show a small FPS counter on the HUD.")
                .define("showFps", true);
        ENABLE_ZOOM = builder.comment("Enable hold-to-zoom.")
                .define("enableZoom", true);
        SHOW_CHUNK_DIAGNOSTICS = builder.comment("Show read-only chunk renderer queue diagnostics on the HUD.")
                .define("showChunkDiagnostics", false);
        ZOOM_FACTOR = builder.comment("Zoom multiplier. Lower values zoom in further.")
                .defineInRange("zoomFactor", 0.20D, 0.05D, 0.75D);
        builder.pop();
        SPEC = builder.build();
    }

    private OptiCoreConfig() {}
}
