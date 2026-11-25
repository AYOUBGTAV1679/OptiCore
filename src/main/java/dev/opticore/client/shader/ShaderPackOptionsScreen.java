package dev.opticore.client.shader;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shader option editor. Values are validated, persisted per pack, and applied
 * to recognized GLSL declarations before a shader program is compiled.
 */
public final class ShaderPackOptionsScreen extends Screen {
    private static final int OPTIONS_PER_PAGE = 5;
    private final Screen parent;
    private final String packName;
    private final List<String> controls;
    private final ShaderOptionParser.Summary discoveredOptions;
    private final List<ShaderOptionParser.Option> editableOptions;
    private final List<String> screenNames;
    private final Map<String, List<String>> nestedScreens;
    private final List<DisplayRow> rows;
    private final Path storePath;
    private final Map<String, String> values = new LinkedHashMap<>();
    private record DisplayRow(String label, boolean heading) {}
    private final String screenDefinition;
    private int scrollOffset;
    private int optionPage;
    /** -1 displays the root/all-options view; non-negative values select screen.<NAME>. */
    private int activeScreenIndex = -1;
    private String saveStatus = "";

    public ShaderPackOptionsScreen(Screen parent, String packName,
                                   ShaderPackMetadata.Summary metadata,
                                   ShaderOptionParser.Summary discoveredOptions) {
        super(Component.translatable("screen.opticore.shaders.options_title"));
        this.parent = parent;
        this.packName = packName;
        this.controls = metadata.controls();
        this.discoveredOptions = discoveredOptions;
        this.screenDefinition = metadata.screenDefinition();
        this.nestedScreens = metadata.nestedScreens();
        this.screenNames = nestedScreens.keySet().stream().sorted().toList();
        this.editableOptions = discoveredOptions.options().values().stream()
                .filter(ShaderOptionParser.Option::editable)
                .sorted(Comparator.comparing(ShaderOptionParser.Option::id)).toList();
        this.rows = buildRows(metadata);
        this.storePath = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("opticore").resolve("shader-pack-options.properties");
        values.putAll(ShaderPackOptionStore.load(storePath, packName));
    }

    private List<DisplayRow> buildRows(ShaderPackMetadata.Summary metadata) {
        List<DisplayRow> display = new ArrayList<>();
        for (ShaderOptionParser.Option option : discoveredOptions.options().values().stream()
                .sorted(Comparator.comparing(ShaderOptionParser.Option::id)).toList()) {
            String valuesText = option.values().isEmpty() ? "" : " values=" + String.join("/", option.values());
            String state = option.ambiguous() ? " [ambiguous]"
                    : option.editable() ? " [editable]" : " [read-only]";
            display.add(new DisplayRow(option.id() + "  "
                    + option.type().name().toLowerCase(java.util.Locale.ROOT)
                    + "  default=" + option.defaultValue() + valuesText + state, false));
            if (!option.description().isBlank()) display.add(new DisplayRow("    " + option.description(), false));
        }
        if (!metadata.nestedScreens().isEmpty()) {
            for (Map.Entry<String, List<String>> group : metadata.nestedScreens().entrySet()) {
                display.add(new DisplayRow("[" + group.getKey() + "]", true));
                for (String option : group.getValue()) display.add(new DisplayRow("  " + option, false));
            }
        } else {
            for (String option : controls) display.add(new DisplayRow(option, false));
        }
        return List.copyOf(display);
    }

    @Override
    protected void init() {
        clampScrollOffset();
        int center = width / 2;
        List<ShaderOptionParser.Option> visibleOptions = visibleOptions();
        if (!screenNames.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("<"), ignored -> changeScreen(-1))
                    .bounds(center - 145, 56, 36, 18).build());
            String screenLabel = activeScreenIndex < 0 ? "All options"
                    : screenNames.get(activeScreenIndex);
            addRenderableWidget(Button.builder(Component.literal(screenLabel), ignored -> {})
                    .bounds(center - 104, 56, 208, 18).build());
            Button nextScreen = Button.builder(Component.literal(">"), ignored -> changeScreen(1))
                    .bounds(center + 109, 56, 36, 18).build();
            addRenderableWidget(nextScreen);
        }
        int start = optionPage * OPTIONS_PER_PAGE;
        int end = Math.min(visibleOptions.size(), start + OPTIONS_PER_PAGE);
        int y = screenNames.isEmpty() ? 70 : 78;
        for (int i = start; i < end; i++) {
            ShaderOptionParser.Option option = visibleOptions.get(i);
            String current = currentValue(option);
            if (option.slider() && option.type() == ShaderOptionParser.Type.NUMBER
                    && option.values().size() > 1) {
                addRenderableWidget(new DiscreteOptionSlider(center - 150, y, 300, 20, option, current));
            } else {
                Button button = Button.builder(Component.literal(option.id() + ": " + current),
                        ignored -> cycle(option)).bounds(center - 150, y, 300, 20).build();
                addRenderableWidget(button);
            }
            y += 21;
        }
        int navY = height - 55;
        Button previous = Button.builder(Component.literal("<"), ignored -> changeOptionPage(-1))
                .bounds(center - 150, navY, 70, 20).build();
        previous.active = optionPage > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal(">"), ignored -> changeOptionPage(1))
                .bounds(center + 80, navY, 70, 20).build();
        next.active = end < visibleOptions.size();
        addRenderableWidget(next);
        addRenderableWidget(Button.builder(Component.literal("Reset pack"),
                ignored -> resetPackOptions()).bounds(center - 150, height - 29, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.done"),
                ignored -> onClose()).bounds(center + 50, height - 29, 100, 20).build());
    }

    private void resetPackOptions() {
        if (ShaderPackOptionStore.clearPack(storePath, packName)) {
            values.clear();
            saveStatus = "Pack options reset";
        } else {
            saveStatus = "Reset failed";
        }
        rebuildWidgets();
    }

    private String currentValue(ShaderOptionParser.Option option) {
        String stored = values.get(option.id());
        if (stored != null && option.values().contains(stored)) return stored;
        return option.defaultValue();
    }

    private void cycle(ShaderOptionParser.Option option) {
        List<String> choices = option.values();
        if (choices.isEmpty()) return;
        String current = currentValue(option);
        int index = choices.indexOf(current);
        String next = choices.get((index + 1 + choices.size()) % choices.size());
        setOptionValue(option, next);
        rebuildWidgets();
    }

    private boolean setOptionValue(ShaderOptionParser.Option option, String next) {
        if (!option.values().contains(next)) return false;
        if (next.equals(option.defaultValue())) {
            // Keep the in-memory override until persistence succeeds; otherwise the
            // label would show the default while the previous override remains on disk.
            if (ShaderPackOptionStore.save(storePath, packName, option.id(), null)) {
                values.remove(option.id());
                saveStatus = "Saved (default)";
                return true;
            }
        } else if (ShaderPackOptionStore.save(storePath, packName, option.id(), next)) {
            values.put(option.id(), next);
            saveStatus = "Saved";
            return true;
        }
        saveStatus = "Save failed";
        return false;
    }

    private final class DiscreteOptionSlider extends AbstractSliderButton {
        private final ShaderOptionParser.Option option;

        private DiscreteOptionSlider(int x, int y, int width, int height,
                                     ShaderOptionParser.Option option, String selected) {
            super(x, y, width, height, Component.empty(),
                    ShaderOptionSliderModel.position(option.values(), selected, option.defaultValue()));
            this.option = option;
            updateMessage();
        }

        private double positionFor(String selected) {
            return ShaderOptionSliderModel.position(option.values(), selected, option.defaultValue());
        }

        private String selectedValue() {
            return ShaderOptionSliderModel.valueAt(option.values(), value);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(option.id() + ": " + selectedValue()));
        }

        @Override
        protected void applyValue() {
            if (!setOptionValue(option, selectedValue())) {
                value = positionFor(currentValue(option));
                updateMessage();
            }
        }
    }

    private List<ShaderOptionParser.Option> visibleOptions() {
        if (activeScreenIndex < 0 || activeScreenIndex >= screenNames.size()) return editableOptions;
        Set<String> identifiers = new LinkedHashSet<>();
        List<String> declared = nestedScreens.getOrDefault(screenNames.get(activeScreenIndex), List.of());
        for (String token : declared) {
            String normalized = token.replaceFirst("^[!*]+", "");
            if (normalized.matches("[A-Za-z_][A-Za-z0-9_]*")) identifiers.add(normalized);
        }
        return editableOptions.stream().filter(option -> identifiers.contains(option.id())).toList();
    }

    private void changeScreen(int delta) {
        int count = screenNames.size() + 1;
        int current = activeScreenIndex + 1;
        activeScreenIndex = Math.floorMod(current + delta, count) - 1;
        optionPage = 0;
        rebuildWidgets();
    }

    private void changeOptionPage(int delta) {
        List<ShaderOptionParser.Option> visible = visibleOptions();
        int pageCount = Math.max(1, (visible.size() + OPTIONS_PER_PAGE - 1) / OPTIONS_PER_PAGE);
        optionPage = Math.max(0, Math.min(pageCount - 1, optionPage + delta));
        rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listTop = 185;
        int listBottom = Math.max(listTop + 1, height - 105);
        if (mouseX < 12 || mouseX >= width - 12 || mouseY < listTop || mouseY >= listBottom) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int max = Math.max(0, rows.size() - visibleRows());
        scrollOffset = (int) Math.max(0, Math.min(max, scrollOffset - Math.signum(delta) * 3));
        return true;
    }

    private int visibleRows() { return Math.max(1, (height - 290) / 12); }

    private void clampScrollOffset() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, rows.size() - visibleRows())));
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(packName), width / 2, 27, 0xC8E6FF);
        graphics.drawCenteredString(font, Component.literal("Editable declarations — saved locally"),
                width / 2, 43, 0xA6D8A8);
        if (!screenDefinition.isBlank() && screenNames.isEmpty()) {
            String declaration = screenDefinition.length() > 100
                    ? screenDefinition.substring(0, 97) + "..." : screenDefinition;
            graphics.drawCenteredString(font, Component.literal(declaration), width / 2, 57, 0xB8B8B8);
        }
        if (discoveredOptions.truncated()) {
            graphics.drawCenteredString(font,
                    Component.literal("Warning: shader option inventory is incomplete (scan limits reached)"),
                    width / 2, 166, 0xFFCC66);
        }
        List<ShaderOptionParser.Option> visibleOptions = visibleOptions();
        int pageCount = Math.max(1, (visibleOptions.size() + OPTIONS_PER_PAGE - 1) / OPTIONS_PER_PAGE);
        graphics.drawCenteredString(font,
                Component.literal("Options " + (visibleOptions.isEmpty() ? 0 : optionPage + 1)
                        + "/" + pageCount + "  •  " + visibleOptions.size() + " editable"),
                width / 2, height - 68, 0xAAAAAA);
        if (!saveStatus.isEmpty()) graphics.drawCenteredString(font, Component.literal(saveStatus),
                width / 2, height - 82, saveStatus.equals("Save failed") ? 0xFF7777 : 0xA6D8A8);

        graphics.drawCenteredString(font,
                Component.literal("Detected declarations: " + rows.size()
                        + "  •  Recognized overrides are applied when a shader program is compiled."),
                width / 2, height - 96, 0x888888);

        // The declaration inventory was already scrollable, but its rows were never
        // painted. Keep it below the editable controls and above the footer, clipping
        // the text so long inventories cannot overlap the page/status controls.
        int listTop = 185;
        int listBottom = Math.max(listTop + 1, height - 105);
        graphics.enableScissor(12, listTop, Math.max(13, width - 12), listBottom);
        int rowY = listTop + 2 - scrollOffset * 12;
        for (DisplayRow row : rows) {
            if (rowY >= listTop - 12 && rowY < listBottom) {
                graphics.drawString(font, Component.literal(row.label()), 16, rowY,
                        row.heading() ? 0xC8E6FF : 0xB8B8B8, false);
            }
            rowY += 12;
        }
        graphics.disableScissor();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
