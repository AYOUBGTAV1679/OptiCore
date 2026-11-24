package dev.opticore.client.shader;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Shader pack manager for discovery, metadata preview, and persistent selection. */
public final class ShaderPackScreen extends Screen {
    private final Screen parent;
    private List<ShaderPackRepository.Pack> packs = List.of();
    private String selected = "OFF";
    private String statusKey = "screen.opticore.shaders.status_ready";
    private ShaderPackMetadata.Summary selectedMetadata = ShaderPackMetadata.Summary.empty();
    private ShaderPackInventory.Summary selectedInventory = ShaderPackInventory.Summary.empty();

    public ShaderPackScreen(Screen parent) {
        super(Component.translatable("screen.opticore.shaders.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshPacks();
    }

    private void refreshPacks() {
        clearWidgets();
        packs = ShaderPackRepository.discover();
        selected = ShaderPackRepository.activePack();
        ShaderPackRepository.Pack selectedPack = packs.stream().filter(pack -> pack.name().equals(selected))
                .findFirst().orElse(null);
        selectedMetadata = selectedPack == null ? ShaderPackMetadata.Summary.empty()
                : ShaderPackMetadata.inspect(selectedPack);
        selectedInventory = selectedPack == null ? ShaderPackInventory.Summary.empty()
                : ShaderPackInventory.inspect(selectedPack);

        int center = width / 2;
        int y = 48;
        addRenderableWidget(Button.builder(selectionText("OFF"), button -> choose("OFF"))
                .bounds(center - 150, y, 300, 20).build());
        y += 25;

        int visible = Math.max(0, Math.min(packs.size(), (height - 164) / 24));
        for (int i = 0; i < visible; i++) {
            ShaderPackRepository.Pack pack = packs.get(i);
            String label = pack.name() + (pack.archive() ? "  [ZIP]" : "  [Folder]");
            addRenderableWidget(Button.builder(selectionText(pack.name(), label),
                    button -> choose(pack.name()))
                    .bounds(center - 150, y, 300, 20).build());
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.shaders.refresh"),
                button -> refreshPacks()).bounds(center - 150, height - 52, 70, 20).build());
        Button optionsButton = Button.builder(Component.translatable("screen.opticore.shaders.options"),
                button -> minecraft.setScreen(new ShaderPackOptionsScreen(this, selected, selectedMetadata)))
                .bounds(center - 76, height - 52, 70, 20).build();
        optionsButton.active = selectedMetadata.hasProperties();
        addRenderableWidget(optionsButton);
        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.shaders.open_folder"),
                button -> openFolder()).bounds(center - 2, height - 52, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.done"),
                button -> onClose()).bounds(center + 92, height - 52, 58, 20).build());
    }

    private Component selectionText(String name) {
        return selectionText(name, name);
    }

    private Component selectionText(String selectionKey, String label) {
        return Component.literal((selected.equals(selectionKey) ? "✓  " : "   ") + label);
    }

    private void choose(String name) {
        if (ShaderPackRepository.select(name)) {
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
    public void onClose() {
        minecraft.setScreen(parent);
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
                width / 2, height - 91, 0xAAAAAA);
        graphics.drawCenteredString(font, Component.translatable(statusKey),
                width / 2, height - 78, 0xAAAAAA);
        if (selectedInventory.sourceFiles() > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.opticore.shaders.sources", selectedInventory.sourceFiles(),
                            selectedInventory.vertexFiles(), selectedInventory.fragmentFiles()),
                    width / 2, height - 48, 0xA6D8A8);
        }
        if (selectedMetadata.hasProperties()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.opticore.shaders.metadata", selectedMetadata.propertyCount(), selectedMetadata.sliderCount()),
                    width / 2, height - 64, 0xA6D8A8);
        } else if (!"OFF".equals(selected)) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.opticore.shaders.no_metadata"),
                    width / 2, height - 64, 0xAAAAAA);
        }
        if (packs.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.opticore.shaders.empty"),
                    width / 2, height / 2, 0xFFFFFF);
        } else if (packs.size() > Math.max(0, (height - 164) / 24)) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.opticore.shaders.list_limited"),
                    width / 2, height - 105, 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
