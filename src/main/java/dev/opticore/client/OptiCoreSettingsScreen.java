package dev.opticore.client;

import dev.opticore.OptiCoreConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class OptiCoreSettingsScreen extends Screen {
    public OptiCoreSettingsScreen() {
        super(Component.translatable("screen.opticore.title"));
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int top = this.height / 4;

        addRenderableWidget(Button.builder(toggleText("screen.opticore.zoom", OptiCoreConfig.ENABLE_ZOOM.get()), button -> {
            OptiCoreConfig.ENABLE_ZOOM.set(!OptiCoreConfig.ENABLE_ZOOM.get());
            button.setMessage(toggleText("screen.opticore.zoom", OptiCoreConfig.ENABLE_ZOOM.get()));
        }).bounds(center - 105, top, 210, 20).build());

        addRenderableWidget(Button.builder(toggleText("screen.opticore.fps", OptiCoreConfig.SHOW_FPS.get()), button -> {
            OptiCoreConfig.SHOW_FPS.set(!OptiCoreConfig.SHOW_FPS.get());
            button.setMessage(toggleText("screen.opticore.fps", OptiCoreConfig.SHOW_FPS.get()));
        }).bounds(center - 105, top + 28, 210, 20).build());

        addRenderableWidget(Button.builder(toggleText("screen.opticore.chunk_diagnostics", OptiCoreConfig.SHOW_CHUNK_DIAGNOSTICS.get()), button -> {
            OptiCoreConfig.SHOW_CHUNK_DIAGNOSTICS.set(!OptiCoreConfig.SHOW_CHUNK_DIAGNOSTICS.get());
            button.setMessage(toggleText("screen.opticore.chunk_diagnostics", OptiCoreConfig.SHOW_CHUNK_DIAGNOSTICS.get()));
        }).bounds(center - 105, top + 56, 210, 20).build());

        addRenderableWidget(Button.builder(cacheBudgetText(), button -> {
            int current = OptiCoreConfig.RENDER_CACHE_BUDGET_MB.get();
            int next = current >= 256 ? 32 : current * 2;
            OptiCoreConfig.RENDER_CACHE_BUDGET_MB.set(next);
            ChunkRenderCache.applyConfiguredBudget();
            button.setMessage(cacheBudgetText());
        }).bounds(center - 105, top + 84, 210, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Compatibility"), button ->
                minecraft.setScreen(new OptiCoreCompatibilityScreen(this)))
                .bounds(center - 105, top + 112, 210, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.done"), button -> onClose())
                .bounds(center - 105, top + 168, 210, 20).build());
    }

    private static Component cacheBudgetText() {
        return Component.literal("Render cache budget: "
                + OptiCoreConfig.RENDER_CACHE_BUDGET_MB.get() + " MiB");
    }

    private static Component toggleText(String key, boolean enabled) {
        return Component.translatable(key,
                Component.translatable(enabled ? "screen.opticore.on" : "screen.opticore.off"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 4 - 28, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.opticore.zoom_hint"),
                width / 2, height / 4 + 65, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
