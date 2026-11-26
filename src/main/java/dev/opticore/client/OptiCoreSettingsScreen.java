package dev.opticore.client;

import dev.opticore.OptiCoreConfig;
import dev.opticore.client.shader.ShaderPackScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class OptiCoreSettingsScreen extends Screen {
    private static final int COLUMN_WIDTH = 147;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;

    public OptiCoreSettingsScreen() {
        super(Component.translatable("screen.opticore.title"));
    }

    @Override
    protected void init() {
        int center = width / 2;
        int left = center - COLUMN_WIDTH - 3;
        int right = center + 3;
        int top = 52;

        addToggle(left, top, "screen.opticore.zoom", OptiCoreConfig.ENABLE_ZOOM,
                () -> OptiCoreConfig.ENABLE_ZOOM.get());
        addToggle(right, top, "screen.opticore.fps", OptiCoreConfig.SHOW_FPS,
                () -> OptiCoreConfig.SHOW_FPS.get());

        addToggle(left, rowY(top, 1), "screen.opticore.smooth_zoom", OptiCoreConfig.SMOOTH_ZOOM,
                () -> OptiCoreConfig.SMOOTH_ZOOM.get());
        addToggle(right, rowY(top, 1), "screen.opticore.frame_time", OptiCoreConfig.SHOW_FRAME_TIME,
                () -> OptiCoreConfig.SHOW_FRAME_TIME.get(), () -> {
                    if (!OptiCoreConfig.SHOW_FRAME_TIME.get()) FrameTimeMonitor.reset();
                });

        addRenderableWidget(Button.builder(zoomFactorText(), button -> {
            double current = OptiCoreConfig.ZOOM_FACTOR.get();
            double next = current >= 0.75D ? 0.10D
                    : Math.min(0.75D, Math.round((current + 0.05D) * 100.0D) / 100.0D);
            OptiCoreConfig.ZOOM_FACTOR.set(next);
            button.setMessage(zoomFactorText());
        }).bounds(left, rowY(top, 2), COLUMN_WIDTH, ROW_HEIGHT).build());

        addRenderableWidget(Button.builder(cacheBudgetText(), button -> {
            int current = OptiCoreConfig.RENDER_CACHE_BUDGET_MB.get();
            int next = current >= 512 ? 32 : Math.min(512, current * 2);
            OptiCoreConfig.RENDER_CACHE_BUDGET_MB.set(next);
            ChunkRenderCache.applyConfiguredBudget();
            button.setMessage(cacheBudgetText());
        }).bounds(right, rowY(top, 2), COLUMN_WIDTH, ROW_HEIGHT).build());

        addToggle(left, rowY(top, 3), "screen.opticore.chunk_diagnostics",
                OptiCoreConfig.SHOW_CHUNK_DIAGNOSTICS,
                () -> OptiCoreConfig.SHOW_CHUNK_DIAGNOSTICS.get());
        addToggle(right, rowY(top, 3), "screen.opticore.capture_scene",
                OptiCoreConfig.CAPTURE_SCENE_UNIFORMS,
                () -> OptiCoreConfig.CAPTURE_SCENE_UNIFORMS.get());

        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.compatibility"), button ->
                minecraft.setScreen(new OptiCoreCompatibilityScreen(this)))
                .bounds(left, rowY(top, 4), COLUMN_WIDTH, ROW_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.shaders.open"), button ->
                minecraft.setScreen(new ShaderPackScreen(this)))
                .bounds(right, rowY(top, 4), COLUMN_WIDTH, ROW_HEIGHT).build());

        addToggle(left, rowY(top, 5), "screen.opticore.experimental_world_pass",
                OptiCoreConfig.ENABLE_EXPERIMENTAL_WORLD_PASS,
                () -> OptiCoreConfig.ENABLE_EXPERIMENTAL_WORLD_PASS.get());
        addRenderableWidget(Button.builder(fogStrengthText(), button -> {
            double[] levels = {0.0D, 0.10D, 0.22D, 0.40D, 0.65D, 1.0D};
            double current = OptiCoreConfig.WORLD_FOG_STRENGTH.get();
            int next = nextLevel(levels, current);
            OptiCoreConfig.WORLD_FOG_STRENGTH.set(levels[next]);
            button.setMessage(fogStrengthText());
        }).bounds(right, rowY(top, 5), COLUMN_WIDTH, ROW_HEIGHT).build());

        addRenderableWidget(Button.builder(warmthText(), button -> {
            double[] levels = {0.0D, 0.04D, 0.08D, 0.12D, 0.18D, 0.25D};
            double current = OptiCoreConfig.WORLD_COLOR_WARMTH.get();
            int next = nextLevel(levels, current);
            OptiCoreConfig.WORLD_COLOR_WARMTH.set(levels[next]);
            button.setMessage(warmthText());
        }).bounds(left, rowY(top, 6), COLUMN_WIDTH, ROW_HEIGHT).build());

        addRenderableWidget(Button.builder(fogStartText(), button -> {
            double[] levels = {16D, 24D, 32D, 48D, 64D, 96D, 128D};
            double current = OptiCoreConfig.WORLD_FOG_START.get();
            int next = nextLevel(levels, current);
            double selected = levels[next];
            OptiCoreConfig.WORLD_FOG_START.set(selected);
            if (OptiCoreConfig.WORLD_FOG_END.get() <= selected) {
                OptiCoreConfig.WORLD_FOG_END.set(Math.min(4096D, selected + 64D));
            }
            button.setMessage(fogStartText());
        }).bounds(left, rowY(top, 7), COLUMN_WIDTH, ROW_HEIGHT).build());

        addRenderableWidget(Button.builder(fogEndText(), button -> {
            double[] levels = {64D, 96D, 128D, 192D, 256D, 384D, 512D, 768D, 1024D};
            double current = OptiCoreConfig.WORLD_FOG_END.get();
            int next = nextLevel(levels, current);
            double selected = levels[next];
            OptiCoreConfig.WORLD_FOG_END.set(Math.max(
                    selected, OptiCoreConfig.WORLD_FOG_START.get() + 1D));
            button.setMessage(fogEndText());
        }).bounds(right, rowY(top, 7), COLUMN_WIDTH, ROW_HEIGHT).build());


        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.color.open"), button ->
                minecraft.setScreen(new OptiCoreColorSettingsScreen(this)))
                .bounds(center - 150, height - 56, 300, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.done"), button -> onClose())
                .bounds(center - 60, height - 28, 120, 20).build());
    }

    private static int nextLevel(double[] levels, double current) {
        for (int i = 0; i < levels.length; i++) {
            if (Math.abs(levels[i] - current) < 0.001D) return (i + 1) % levels.length;
        }
        return 0;
    }

    private void addToggle(int x, int y, String key, net.minecraftforge.common.ForgeConfigSpec.BooleanValue value,
                           java.util.function.BooleanSupplier current) {
        addToggle(x, y, key, value, current, () -> {});
    }

    private void addToggle(int x, int y, String key, net.minecraftforge.common.ForgeConfigSpec.BooleanValue value,
                           java.util.function.BooleanSupplier current, Runnable afterChange) {
        addRenderableWidget(Button.builder(toggleText(key, current.getAsBoolean()), button -> {
            value.set(!current.getAsBoolean());
            afterChange.run();
            button.setMessage(toggleText(key, current.getAsBoolean()));
        }).bounds(x, y, COLUMN_WIDTH, ROW_HEIGHT).build());
    }

    private static int rowY(int top, int row) {
        return top + row * (ROW_HEIGHT + ROW_GAP);
    }

    private static Component toggleText(String key, boolean enabled) {
        return Component.translatable(key,
                Component.translatable(enabled ? "screen.opticore.on" : "screen.opticore.off"));
    }

    private static Component zoomFactorText() {
        return Component.translatable("screen.opticore.zoom_factor",
                String.format(Locale.ROOT, "%.2fx", OptiCoreConfig.ZOOM_FACTOR.get()));
    }

    private static Component cacheBudgetText() {
        return Component.translatable("screen.opticore.cache_budget",
                OptiCoreConfig.RENDER_CACHE_BUDGET_MB.get());
    }

    private static Component fogStrengthText() {
        return Component.translatable("screen.opticore.world_fog_strength",
                String.format(Locale.ROOT, "%.2f", OptiCoreConfig.WORLD_FOG_STRENGTH.get()));
    }

    private static Component warmthText() {
        return Component.translatable("screen.opticore.world_color_warmth",
                String.format(Locale.ROOT, "%.2f", OptiCoreConfig.WORLD_COLOR_WARMTH.get()));
    }

    private static Component fogStartText() {
        return Component.translatable("screen.opticore.world_fog_start",
                String.format(Locale.ROOT, "%.0f", OptiCoreConfig.WORLD_FOG_START.get()));
    }

    private static Component fogEndText() {
        return Component.translatable("screen.opticore.world_fog_end",
                String.format(Locale.ROOT, "%.0f", OptiCoreConfig.WORLD_FOG_END.get()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int center = width / 2;
        int left = center - COLUMN_WIDTH - 3;
        int right = center + 3;
        graphics.fill(left - 4, 38, left + COLUMN_WIDTH + 4, height - 36, 0x66000000);
        graphics.fill(right - 4, 38, right + COLUMN_WIDTH + 4, height - 36, 0x66000000);
        graphics.drawCenteredString(font, title, center, 14, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.opticore.section.visual"),
                left + COLUMN_WIDTH / 2, 42, 0xA9D8FF);
        graphics.drawCenteredString(font, Component.translatable("screen.opticore.section.performance"),
                right + COLUMN_WIDTH / 2, 42, 0xA9D8FF);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, Component.translatable("screen.opticore.zoom_hint"),
                center, height - 70, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
