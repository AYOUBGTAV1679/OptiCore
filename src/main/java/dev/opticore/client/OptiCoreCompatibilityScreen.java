package dev.opticore.client;

import dev.opticore.compat.ModCompatibilityScanner;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class OptiCoreCompatibilityScreen extends Screen {
    private final Screen parent;
    private List<ModCompatibilityScanner.Entry> entries = List.of();
    private int firstVisible;

    public OptiCoreCompatibilityScreen(Screen parent) {
        super(Component.literal("OptiCore Compatibility"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        entries = ModCompatibilityScanner.scan();
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> minecraft.setScreen(parent))
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.literal("Presence scan only — detected does not mean compatible"),
                width / 2, 22, 0xFFCC55);

        int rowHeight = 27;
        int maxRows = Math.max(1, (height - 78) / rowHeight);
        int maxStart = Math.max(0, entries.size() - maxRows);
        firstVisible = Math.max(0, Math.min(firstVisible, maxStart));

        int y = 40;
        int end = Math.min(entries.size(), firstVisible + maxRows);
        for (int i = firstVisible; i < end; i++) {
            ModCompatibilityScanner.Entry entry = entries.get(i);
            graphics.drawString(font,
                    entry.displayName() + " [" + entry.modId() + "]",
                    12, y, 0xFFFFFF);

            String status = entry.excluded()
                    ? (entry.present() ? "EXCLUDED — INSTALLED" : "EXCLUDED")
                    : (entry.present() ? "DETECTED" : "NOT DETECTED");
            int statusColor = entry.excluded() ? 0xFF6666
                    : (entry.present() ? 0x66FF66 : 0xAAAAAA);
            graphics.drawString(font, status, width - 125, y, statusColor);
            graphics.drawString(font, entry.note(), 20, y + 11, 0x999999);
            y += rowHeight;
        }

        graphics.drawCenteredString(font,
                Component.literal((entries.isEmpty() ? 0 : firstVisible + 1)
                        + "–" + end + " / " + entries.size() + "  (scroll to view)"),
                width / 2, height - 42, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int rowHeight = 27;
        int maxRows = Math.max(1, (height - 78) / rowHeight);
        int maxStart = Math.max(0, entries.size() - maxRows);
        firstVisible = Math.max(0, Math.min(maxStart, firstVisible - (int) Math.signum(delta) * 3));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
