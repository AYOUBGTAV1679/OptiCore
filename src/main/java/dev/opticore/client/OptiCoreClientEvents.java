package dev.opticore.client;

import dev.opticore.OptiCore;
import dev.opticore.OptiCoreConfig;
import dev.opticore.client.shader.SceneUniformSnapshot;
import dev.opticore.client.shader.SceneEnvironmentSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OptiCore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OptiCoreClientEvents {
    private static volatile SceneUniformSnapshot latestSceneUniformSnapshot;
    private static volatile SceneEnvironmentSnapshot latestSceneEnvironmentSnapshot;

    private OptiCoreClientEvents() {}

    /** Latest read-only scene snapshot; null until capture is enabled and a level frame completes. */
    public static SceneUniformSnapshot latestSceneUniformSnapshot() {
        return latestSceneUniformSnapshot;
    }

    /** Latest fog, weather, and camera light sample; null until capture is enabled. */
    public static SceneEnvironmentSnapshot latestSceneEnvironmentSnapshot() {
        return latestSceneEnvironmentSnapshot;
    }

    /**
     * Observe the completed level stage without changing render state or issuing GL calls.
     * Capture is opt-in and disabled by default.
     */
    @SubscribeEvent
    public static void onLevelRenderStage(RenderLevelStageEvent event) {
        if (!OptiCoreConfig.CAPTURE_SCENE_UNIFORMS.get()
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            latestSceneUniformSnapshot = null;
            latestSceneEnvironmentSnapshot = null;
            return;
        }

        float[] modelView = new float[16];
        float[] projection = new float[16];
        event.getPoseStack().last().pose().get(modelView);
        event.getProjectionMatrix().get(projection);
        net.minecraft.world.phys.Vec3 camera = event.getCamera().getPosition();
        latestSceneUniformSnapshot = new SceneUniformSnapshot(
                modelView, projection,
                camera.x, camera.y, camera.z,
                event.getPartialTick(), event.getRenderTick(),
                mc.getWindow().getWidth(), mc.getWindow().getHeight(),
                mc.level.getGameTime(), mc.level.getDayTime(),
                mc.level.dimension().location().toString());

        net.minecraft.world.phys.Vec3 skyColor = mc.level.getSkyColor(camera, event.getPartialTick());
        BlockPos cameraBlock = BlockPos.containing(camera.x, camera.y, camera.z);
        latestSceneEnvironmentSnapshot = new SceneEnvironmentSnapshot(
                (float) skyColor.x, (float) skyColor.y, (float) skyColor.z,
                mc.level.getRainLevel(event.getPartialTick()),
                mc.level.getThunderLevel(event.getPartialTick()),
                mc.level.getSkyDarken() / 15.0f,
                mc.level.getBrightness(LightLayer.BLOCK, cameraBlock),
                mc.level.getBrightness(LightLayer.SKY, cameraBlock));

    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ChunkRenderCache.applyConfiguredBudget();
        ChunkDiagnostics.tick(mc);

        while (OptiCoreClient.OPEN_SETTINGS.consumeClick()) {
            mc.setScreen(new OptiCoreSettingsScreen());
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (!OptiCoreConfig.ENABLE_ZOOM.get()) {
            ZoomController.reset();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            ZoomController.reset();
            return;
        }
        double factor = ZoomController.update(OptiCoreClient.ZOOM.isDown(),
                OptiCoreConfig.ZOOM_FACTOR.get(), OptiCoreConfig.SMOOTH_ZOOM.get());
        event.setNewFovModifier((float) (event.getFovModifier() * factor));
    }

    @SubscribeEvent
    public static void onHudRender(RenderGuiOverlayEvent.Post event) {
        // Draw once per frame, after the hotbar overlay.
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        boolean showFps = OptiCoreConfig.SHOW_FPS.get();
        boolean showChunkDiagnostics = OptiCoreConfig.SHOW_CHUNK_DIAGNOSTICS.get();
        boolean showFrameTime = OptiCoreConfig.SHOW_FRAME_TIME.get();
        if (showFrameTime) {
            FrameTimeMonitor.sampleFrame();
        } else {
            FrameTimeMonitor.reset();
        }
        if (!showFps && !showChunkDiagnostics && !showFrameTime) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        int y = 6;
        if (showFps) {
            event.getGuiGraphics().drawString(mc.font,
                    Component.literal("OptiCore | " + mc.getFps() + " FPS"), 6, y, 0xFFFFFF, true);
            y += 12;
        }
        if (showFrameTime) {
            event.getGuiGraphics().drawString(mc.font,
                    Component.literal(FrameTimeMonitor.displayText()), 6, y, 0xFFE6A8, true);
            y += 12;
        }
        if (showChunkDiagnostics) {
            ChunkDiagnostics.Snapshot snapshot = ChunkDiagnostics.snapshot();
            event.getGuiGraphics().drawString(mc.font,
                    Component.literal("Chunk queue: " + snapshot.queuedBuilds
                            + " | uploads: " + snapshot.pendingUploads
                            + " | free buffers: " + snapshot.freeBuffers),
                    6, y, 0xB8E6FF, true);
            event.getGuiGraphics().drawString(mc.font,
                    Component.literal("Dispatcher: " + snapshot.dispatcherStats),
                    6, y + 12, 0xB8E6FF, true);
            event.getGuiGraphics().drawString(mc.font,
                    Component.literal("OptiCore cache: " + snapshot.cacheStats.hits() + " hits / "
                            + snapshot.cacheStats.misses() + " misses | "
                            + (snapshot.cacheBytes / 1024L) + " / "
                            + (snapshot.cacheBudgetBytes / 1024L) + " KiB"),
                    6, y + 24, 0xB8E6FF, true);
        }
    }
}
