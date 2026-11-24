package dev.opticore.client;

import dev.opticore.OptiCore;
import dev.opticore.OptiCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OptiCore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OptiCoreClientEvents {
    private OptiCoreClientEvents() {}

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
        if (!OptiCoreConfig.ENABLE_ZOOM.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !OptiCoreClient.ZOOM.isDown()) return;
        float factor = OptiCoreConfig.ZOOM_FACTOR.get().floatValue();
        event.setNewFovModifier(event.getFovModifier() * factor);
    }

    @SubscribeEvent
    public static void onHudRender(RenderGuiOverlayEvent.Post event) {
        // Draw once per frame, after the hotbar overlay.
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        boolean showFps = OptiCoreConfig.SHOW_FPS.get();
        boolean showChunkDiagnostics = OptiCoreConfig.SHOW_CHUNK_DIAGNOSTICS.get();
        if (!showFps && !showChunkDiagnostics) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        int y = 6;
        if (showFps) {
            event.getGuiGraphics().drawString(mc.font,
                    Component.literal("OptiCore | " + mc.getFps() + " FPS"), 6, y, 0xFFFFFF, true);
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
