package dev.opticore.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.opticore.OptiCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Opt-in bridge that captures the completed vanilla level color/depth buffers
 * into an isolated OptiCore framebuffer and applies an opt-in fullscreen GLSL pass
 * at Forge's AFTER_LEVEL stage.
 */
public final class ShaderWorldPassPrototype {
    private static long successfulCaptures;
    private static int lastSuccessfulRenderTick = -1;
    private static String lastFailure = "";
    private static FullscreenCompositePass compositePass = new FullscreenCompositePass();
    private static String loadedPackName;
    private static GlShaderProgram selectedPackProgram;

    private ShaderWorldPassPrototype() {}

    /** Called from the Forge render-stage event on the client render thread. */
    public static void onWorldRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (!OptiCoreConfig.ENABLE_EXPERIMENTAL_WORLD_PASS.get()) return;

        RenderSystem.assertOnRenderThread();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        float fogStrength = OptiCoreConfig.WORLD_FOG_STRENGTH.get().floatValue();
        float warmth = OptiCoreConfig.WORLD_COLOR_WARMTH.get().floatValue();
        // A neutral pass would only copy the full-resolution scene and issue a
        // fullscreen draw. Skip it entirely to avoid needless GPU bandwidth.
        if (fogStrength == 0.0f && warmth == 0.0f) {
            lastFailure = "";
            return;
        }

        refreshSelectedPackProgram();

        RenderTarget vanillaScene = minecraft.getMainRenderTarget();
        GlRenderTarget capturedScene = null;
        boolean sceneCaptured = false;
        try {
            capturedScene = GlRenderTargetManager.acquire(
                    "world-scene-capture", vanillaScene.width, vanillaScene.height);
            capturedScene.copySceneFrom(vanillaScene);
            sceneCaptured = true;
            float[] projection = new float[16];
            event.getProjectionMatrix().get(projection);
            float[] inverseProjection = Matrix4Inverse.invert(projection);
            compositePass.render(capturedScene, vanillaScene, warmth, fogStrength,
                    inverseProjection, selectedPackProgram);
            successfulCaptures++;
            lastSuccessfulRenderTick = event.getRenderTick();
            lastFailure = "";
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            String diagnostic = message == null || message.isBlank()
                    ? failure.getClass().getSimpleName() : message;
            if (sceneCaptured && capturedScene != null) {
                try {
                    capturedScene.copySceneTo(vanillaScene);
                    diagnostic += " (original scene restored)";
                } catch (RuntimeException restoreFailure) {
                    String restoreMessage = restoreFailure.getMessage();
                    diagnostic += " (scene restore also failed: "
                            + (restoreMessage == null || restoreMessage.isBlank()
                            ? restoreFailure.getClass().getSimpleName() : restoreMessage) + ")";
                }
            }
            lastFailure = diagnostic;
        }
    }

    /** Recreates GL objects after Minecraft reloads client resources. */
    public static void onResourcesReloaded() {
        RenderSystem.assertOnRenderThread();
        compositePass.close();
        compositePass = new FullscreenCompositePass();
        loadedPackName = null;
        selectedPackProgram = null;
    }

    /** Invalidates the selected-pack program after selection or option changes. */
    public static void invalidateShaderPackProgram() {
        RenderSystem.assertOnRenderThread();
        loadedPackName = null;
        selectedPackProgram = null;
    }

    private static void refreshSelectedPackProgram() {
        String selected = ShaderPackRepository.activePack();
        if (selected.equals(loadedPackName)) return;
        loadedPackName = selected;
        selectedPackProgram = null;
        if ("OFF".equalsIgnoreCase(selected)) return;

        ShaderPackRuntime.Result result = ShaderPackRuntime.compileSelectedFullscreenProgram();
        if (result.compiled()) {
            selectedPackProgram = result.program();
            lastFailure = "";
        } else {
            lastFailure = "Shader pack '" + selected + "' is not usable by the fullscreen world pass: "
                    + String.join("; ", result.diagnostics());
        }
    }

    public static long successfulCaptures() { return successfulCaptures; }
    public static int lastSuccessfulRenderTick() { return lastSuccessfulRenderTick; }
    public static String lastFailure() { return lastFailure; }
}
