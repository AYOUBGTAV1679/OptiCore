package dev.opticore;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OptiCoreConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_FPS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ZOOM;
    public static final ForgeConfigSpec.BooleanValue SMOOTH_ZOOM;
    public static final ForgeConfigSpec.BooleanValue SHOW_CHUNK_DIAGNOSTICS;
    public static final ForgeConfigSpec.BooleanValue SHOW_FRAME_TIME;
    public static final ForgeConfigSpec.BooleanValue CAPTURE_SCENE_UNIFORMS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_EXPERIMENTAL_WORLD_PASS;
    public static final ForgeConfigSpec.DoubleValue ZOOM_FACTOR;
    public static final ForgeConfigSpec.IntValue RENDER_CACHE_BUDGET_MB;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        SHOW_FPS = builder.comment("Show a small FPS counter on the HUD.")
                .define("showFps", true);
        ENABLE_ZOOM = builder.comment("Enable hold-to-zoom.")
                .define("enableZoom", true);
        SMOOTH_ZOOM = builder.comment("Smoothly animate zoom in and out.")
                .define("smoothZoom", true);
        SHOW_CHUNK_DIAGNOSTICS = builder.comment("Show read-only chunk renderer queue diagnostics on the HUD.")
                .define("showChunkDiagnostics", false);
        SHOW_FRAME_TIME = builder.comment("Show rolling frame time and estimated 1% low FPS on the HUD.")
                .define("showFrameTime", false);
        CAPTURE_SCENE_UNIFORMS = builder.comment("Capture read-only scene matrices and camera/time data at AFTER_LEVEL for shader integration diagnostics.")
                .define("captureSceneUniforms", false);
        ENABLE_EXPERIMENTAL_WORLD_PASS = builder.comment("Capture vanilla scene color and depth into an isolated OptiCore framebuffer after the level render. No visible composite is applied yet.")
                .define("experimentalWorldPass", false);
        RENDER_CACHE_BUDGET_MB = builder.comment("CPU-side render artifact cache budget in MiB.")
                .defineInRange("renderCacheBudgetMb", 32, 8, 512);
        ZOOM_FACTOR = builder.comment("Zoom multiplier. Lower values zoom in further.")
                .defineInRange("zoomFactor", 0.20D, 0.05D, 0.75D);
        builder.pop();
        SPEC = builder.build();
    }

    private OptiCoreConfig() {}
}
