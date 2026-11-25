package dev.opticore.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.opticore.OptiCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * First opt-in bridge from Forge's world-render lifecycle to OptiCore's GL pass resources.
 *
 * <p>This prototype allocates and clears an isolated half-resolution target after solid
 * terrain has rendered. It deliberately does not sample or replace Minecraft's main
 * framebuffer, draw world geometry, or composite into the visible frame.</p>
 */
public final class ShaderWorldPassPrototype {
    private static long successfulPasses;
    private static int lastSuccessfulRenderTick = -1;
    private static String lastFailure = "";

    private ShaderWorldPassPrototype() {}

    /** Called from the Forge render-stage event on the client render thread. */
    public static void onWorldRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return;
        if (!OptiCoreConfig.ENABLE_EXPERIMENTAL_WORLD_PASS.get()) return;

        RenderSystem.assertOnRenderThread();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        int width = Math.max(1, minecraft.getWindow().getWidth() / 2);
        int height = Math.max(1, minecraft.getWindow().getHeight() / 2);
        try {
            GlRenderTarget target = GlRenderTargetManager.acquire("world-stage-prototype", width, height);
            // This touches only OptiCore's private FBO. GlRenderTarget restores the
            // previous read/draw framebuffer, viewport, clear values and write masks.
            target.clear(0.025f, 0.035f, 0.055f, 0.0f, true);
            successfulPasses++;
            lastSuccessfulRenderTick = event.getRenderTick();
            lastFailure = "";
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            lastFailure = message == null || message.isBlank()
                    ? failure.getClass().getSimpleName() : message;
        }
    }

    public static long successfulPasses() {
        return successfulPasses;
    }

    public static int lastSuccessfulRenderTick() {
        return lastSuccessfulRenderTick;
    }

    public static String lastFailure() {
        return lastFailure;
    }
}
