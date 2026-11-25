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
    private ShaderProgramIndex.Summary selectedPrograms = ShaderProgramIndex.Summary.empty();
    private ShaderOptionParser.Summary selectedOptions = ShaderOptionParser.Summary.empty();
    private int page;
    private int pageSize = 1;
    private boolean initialSelectionLocated;

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
        pageSize = Math.max(1, (height - 190) / 24);
        if (!initialSelectionLocated) {
            for (int i = 0; i < packs.size(); i++) {
                if (packs.get(i).name().equals(selected)) {
                    page = i / pageSize;
                    break;
                }
            }
            initialSelectionLocated = true;
        }
        int pageCount = pageCount();
        page = Math.max(0, Math.min(page, pageCount - 1));

        ShaderPackRepository.Pack selectedPack = packs.stream()
                .filter(pack -> pack.name().equals(selected)).findFirst().orElse(null);
        selectedMetadata = selectedPack == null ? ShaderPackMetadata.Summary.empty()
                : ShaderPackMetadata.inspect(selectedPack);
        selectedInventory = selectedPack == null ? ShaderPackInventory.Summary.empty()
                : ShaderPackInventory.inspect(selectedPack.path(), selectedPack.archive());
        selectedPrograms = selectedPack == null ? ShaderProgramIndex.Summary.empty()
                : ShaderProgramIndex.inspect(selectedPack.path(), selectedPack.archive());
        selectedOptions = selectedPack == null ? ShaderOptionParser.Summary.empty()
                : ShaderOptionDiscovery.inspect(selectedPack.path(), selectedPack.archive());

        int center = width / 2;
        int y = 48;
        addRenderableWidget(Button.builder(selectionText("OFF"), button -> choose("OFF"))
                .bounds(center - 150, y, 300, 20).build());

        int start = page * pageSize;
        int end = Math.min(packs.size(), start + pageSize);
        y += 25;
        for (int i = start; i < end; i++) {
            ShaderPackRepository.Pack pack = packs.get(i);
            String label = pack.name() + (pack.archive() ? "  [ZIP]" : "  [Folder]");
            addRenderableWidget(Button.builder(selectionText(pack.name(), label),
                    button -> choose(pack.name()))
                    .bounds(center - 150, y, 300, 20).build());
            y += 24;
        }

        int navY = height - 78;
        Button previous = Button.builder(Component.translatable("screen.opticore.shaders.previous"),
                button -> changePage(-1)).bounds(center - 150, navY, 145, 20).build();
        previous.active = page > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.translatable("screen.opticore.shaders.next"),
                button -> changePage(1)).bounds(center + 5, navY, 145, 20).build();
        next.active = page + 1 < pageCount();
        addRenderableWidget(next);

        int actionY = height - 29;
        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.shaders.refresh"),
                button -> { page = 0; refreshPacks(); })
                .bounds(center - 150, actionY, 70, 20).build());
        Button optionsButton = Button.builder(Component.translatable("screen.opticore.shaders.options"),
                button -> minecraft.setScreen(new ShaderPackOptionsScreen(this, selected, selectedMetadata, selectedOptions)))
                .bounds(center - 76, actionY, 70, 20).build();
        optionsButton.active = selectedMetadata.hasProperties() || !selectedOptions.options().isEmpty();
        addRenderableWidget(optionsButton);
        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.shaders.open_folder"),
                button -> openFolder()).bounds(center - 2, actionY, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.done"),
                button -> onClose()).bounds(center + 92, actionY, 58, 20).build());
    }

    private int pageCount() {
        return Math.max(1, (packs.size() + pageSize - 1) / pageSize);
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(pageCount() - 1, page + delta));
        refreshPacks();
    }

    private Component selectionText(String name) {
        return selectionText(name, name);
    }

    private Component selectionText(String selectionKey, String label) {
        return Component.literal((selected.equals(selectionKey) ? "✓  " : "   ") + label);
    }

    private void choose(String name) {
        if (!ShaderPackRepository.select(name)) {
            statusKey = "screen.opticore.shaders.status_error";
            refreshPacks();
            return;
        }

        statusKey = "screen.opticore.shaders.status_saved";
        if (!"OFF".equals(name)) {
            ShaderPackRepository.Pack pack = ShaderPackRepository.discover().stream()
                    .filter(candidate -> candidate.name().equals(name))
                    .findFirst().orElse(null);
            ShaderProgramIndex.Summary programs = pack == null ? ShaderProgramIndex.Summary.empty()
                    : ShaderProgramIndex.inspect(pack.path(), pack.archive());
            String firstProgram = programs.paired().stream().sorted().findFirst().orElse(null);
            if (pack == null) {
                statusKey = "Selected pack could not be reopened";
            } else if (firstProgram == null) {
                statusKey = "Selected pack has no paired .vsh/.fsh program";
            } else {
                // Button callbacks execute on Minecraft's render thread. Compile one
                // bounded program immediately so selection validates real GLSL, rather
                // than only saving a pack name and displaying its file inventory.
                ShaderPackRuntime.Result result = ShaderPackRuntime.compileProgram(pack, firstProgram);
                statusKey = result.compiled()
                        ? "GLSL compile/link OK: " + firstProgram
                        : "GLSL compile failed: " + (result.diagnostics().isEmpty()
                                ? "unknown error" : result.diagnostics().get(0));
            }
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
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("screen.opticore.shaders.selected", selected),
                width / 2, 31, 0xC8E6FF);
        graphics.drawCenteredString(font,
                Component.translatable("screen.opticore.shaders.count", packs.size()),
                width / 2, height - 144, 0xAAAAAA);
        graphics.drawCenteredString(font, Component.translatable(statusKey),
                width / 2, height - 128, 0xAAAAAA);
        graphics.drawCenteredString(font,
                Component.translatable("screen.opticore.shaders.page", page + 1, pageCount()),
                width / 2, height - 56, 0xAAAAAA);

        if (selectedInventory.sourceFiles() > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.opticore.shaders.sources", selectedInventory.sourceFiles(),
                            selectedInventory.vertexFiles(), selectedInventory.fragmentFiles(),
                            selectedPrograms.pairedPrograms(), selectedPrograms.vertexOnly(),
                            selectedPrograms.fragmentOnly()),
                    width / 2, height - 96, 0xA6D8A8);
        }
        if (selectedMetadata.hasProperties()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.opticore.shaders.metadata",
                            selectedMetadata.propertyCount(), selectedMetadata.sliderCount()),
                    width / 2, height - 112, 0xA6D8A8);
        } else if (!"OFF".equals(selected)) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.opticore.shaders.no_metadata"),
                    width / 2, height - 112, 0xAAAAAA);
        }
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
