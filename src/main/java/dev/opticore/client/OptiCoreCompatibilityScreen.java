package dev.opticore.client;

import dev.opticore.compat.ModCompatibilityScanner;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public class OptiCoreCompatibilityScreen extends Screen {
    private final Screen parent;
    private List<ModCompatibilityScanner.Entry> entries;

    public OptiCoreCompatibilityScreen(Screen parent) {
        super(Component.literal("OptiCore Compatibility"));
        this.parent = parent;
    }

    @Override protected void init() {
        entries = ModCompatibilityScanner.scan();
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> minecraft.setScreen(parent))
            .bounds(width / 2 - 50, height - 30, 100, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal("Presence check only — not a compatibility test"),
            width / 2, 27, 0xFFCC55);
        int y = 46;
        int maxRows = Math.max(1, (height - 90) / 22);
        for (int i = 0; i < entries.size() && i < maxRows; i++) {
            var e = entries.get(i);
            graphics.drawString(font, e.displayName() + " [" + e.modId() + "]", 18, y, 0xFFFFFF);
            graphics.drawString(font, e.present() ? "DETECTED" : "NOT DETECTED",
                width - 100, y, e.present() ? 0x55FF55 : 0xFF7777);
            y += 22;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
