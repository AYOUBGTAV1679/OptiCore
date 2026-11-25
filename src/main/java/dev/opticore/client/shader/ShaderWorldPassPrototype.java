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

    private ShaderWorldPassPrototype() {}

    /** Called from the Forge render-stage event on the client render thread. */
    public static void onWorldRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (!OptiCoreConfig.ENABLE_EXPERIMENTAL_WORLD_PASS.get()) return;

        RenderSystem.assertOnRenderThread();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        RenderTarget vanillaScene = minecraft.getMainRenderTarget();
        try {
            GlRenderTarget target = GlRenderTargetManager.acquire(
                    "world-scene-capture", vanillaScene.width, vanillaScene.height);
            target.copySceneFrom(vanillaScene);
            compositePass.render(target, vanillaScene, 0.12f,
                    OptiCoreConfig.WORLD_FOG_STRENGTH.get().floatValue());
            successfulCaptures++;
            lastSuccessfulRenderTick = event.getRenderTick();
            lastFailure = "";
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            lastFailure = message == null || message.isBlank()
                    ? failure.getClass().getSimpleName() : message;
        }
    }

    /** Recreates GL objects after Minecraft reloads client resources. */
    public static void onResourcesReloaded() {
        RenderSystem.assertOnRenderThread();
        compositePass.close();
        compositePass = new FullscreenCompositePass();
    }

    public static long successfulCaptures() { return successfulCaptures; }
    public static int lastSuccessfulRenderTick() { return lastSuccessfulRenderTick; }
    public static String lastFailure() { return lastFailure; }
}
