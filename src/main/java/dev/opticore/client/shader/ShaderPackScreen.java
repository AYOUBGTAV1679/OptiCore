package dev.opticore.client.shader;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** First-stage shader pack manager: discovery and persistent selection only. */
public final class ShaderPackScreen extends Screen {
    private List<ShaderPackRepository.Pack> packs = List.of();
    private String selected = "OFF";
    private String statusKey = "screen.opticore.shaders.status_ready";

    public ShaderPackScreen(Screen parent) {
        super(Component.translatable("screen.opticore.shaders.title"));
    }

    @Override
    protected void init() {
        refreshPacks();
    }

    private void refreshPacks() {
        clearWidgets();
        packs = ShaderPackRepository.discover();
        selected = ShaderPackRepository.activePack();

        int center = width / 2;
        int y = 48;
        addRenderableWidget(Button.builder(selectionText("OFF"), button -> choose("OFF"))
                .bounds(center - 150, y, 300, 20).build());
        y += 25;

        int visible = Math.max(1, Math.min(packs.size(), (height - 125) / 24));
        for (int i = 0; i < visible; i++) {
            ShaderPackRepository.Pack pack = packs.get(i);
            String label = pack.name() + (pack.archive() ? "  [ZIP]" : "  [Folder]");
            addRenderableWidget(Button.builder(selectionText(label), button -> choose(pack.name()))
                    .bounds(center - 150, y, 300, 20).build());
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.shaders.refresh"),
                button -> refreshPacks()).bounds(center - 150, height - 52, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.shaders.open_folder"),
                button -> openFolder()).bounds(center - 50, height - 52, 112, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.done"),
                button -> onClose()).bounds(center + 66, height - 52, 84, 20).build());
    }

    private Component selectionText(String name) {
        return Component.literal((selected.equals(name) ? "✓  " : "   ") + name);
    }

    private void choose(String name) {
        if (ShaderPackRepository.select(name)) {
            selected = name;
            statusKey = "screen.opticore.shaders.status_saved";
        } else {
            statusKey = "screen.opticore.shaders.status_error";
        }
        refreshPacks();
    }

    private void openFolder() {
        try {
            java.nio.file.Path folder = minecraft.gameDirectory.toPath().resolve("shaderpacks");
            java.nio.file.Files.createDirectories(folder);
            net.minecraft.Util.getPlatform().openFile(folder.toFile());
        } catch (Exception ignored) {
            statusKey = "screen.opticore.shaders.status_error";
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("screen.opticore.shaders.selected", selected),
                width / 2, 32, 0xC8E6FF);
        graphics.drawCenteredString(font,
                Component.translatable("screen.opticore.shaders.count", packs.size()),
                width / 2, height - 76, 0xAAAAAA);
        graphics.drawCenteredString(font, Component.translatable(statusKey),
                width / 2, height - 65, 0xAAAAAA);
        if (packs.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.opticore.shaders.empty"),
                    width / 2, height / 2, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
