package dev.opticore.client;

import dev.opticore.OptiCoreConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Controls for the experimental fullscreen world color-grading stage. */
public final class OptiCoreColorSettingsScreen extends Screen {
    private final Screen parent;

    public OptiCoreColorSettingsScreen(Screen parent) {
        super(Component.translatable("screen.opticore.color.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = width / 2;
        int y = 76;
        addRenderableWidget(Button.builder(exposureText(), button -> {
            double[] levels = {0.5D, 0.75D, 1.0D, 1.25D, 1.5D};
            OptiCoreConfig.WORLD_EXPOSURE.set(next(levels, OptiCoreConfig.WORLD_EXPOSURE.get()));
            button.setMessage(exposureText());
        }).bounds(center - 150, y, 300, 22).build());

        addRenderableWidget(Button.builder(contrastText(), button -> {
            double[] levels = {0.5D, 0.75D, 1.0D, 1.25D, 1.5D};
            OptiCoreConfig.WORLD_CONTRAST.set(next(levels, OptiCoreConfig.WORLD_CONTRAST.get()));
            button.setMessage(contrastText());
        }).bounds(center - 150, y + 30, 300, 22).build());

        addRenderableWidget(Button.builder(saturationText(), button -> {
            double[] levels = {0.0D, 0.5D, 1.0D, 1.5D, 2.0D};
            OptiCoreConfig.WORLD_SATURATION.set(next(levels, OptiCoreConfig.WORLD_SATURATION.get()));
            button.setMessage(saturationText());
        }).bounds(center - 150, y + 60, 300, 22).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.done"),
                button -> onClose()).bounds(center - 60, height - 32, 120, 20).build());
    }

    private static double next(double[] levels, double current) {
        for (int i = 0; i < levels.length; i++) {
            if (Math.abs(levels[i] - current) < 0.001D) return levels[(i + 1) % levels.length];
        }
        return levels[0];
    }

    private static Component exposureText() {
        return Component.translatable("screen.opticore.color.exposure",
                String.format(Locale.ROOT, "%.2f", OptiCoreConfig.WORLD_EXPOSURE.get()));
    }

    private static Component contrastText() {
        return Component.translatable("screen.opticore.color.contrast",
                String.format(Locale.ROOT, "%.2f", OptiCoreConfig.WORLD_CONTRAST.get()));
    }

    private static Component saturationText() {
        return Component.translatable("screen.opticore.color.saturation",
                String.format(Locale.ROOT, "%.2f", OptiCoreConfig.WORLD_SATURATION.get()));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
