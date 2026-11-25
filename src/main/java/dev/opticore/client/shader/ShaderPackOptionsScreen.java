package dev.opticore.client.shader;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shader option editor. Values are validated and persisted per pack.
 * Applying stored values to GLSL compilation is deliberately not claimed here.
 */
public final class ShaderPackOptionsScreen extends Screen {
    private static final int OPTIONS_PER_PAGE = 7;
    private final Screen parent;
    private final String packName;
    private final List<String> controls;
    private final ShaderOptionParser.Summary discoveredOptions;
    private final List<ShaderOptionParser.Option> editableOptions;
    private final List<DisplayRow> rows;
    private final Path storePath;
    private final Map<String, String> values = new LinkedHashMap<>();
    private record DisplayRow(String label, boolean heading) {}
    private final String screenDefinition;
    private int scrollOffset;
    private int optionPage;
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
        int center = width / 2;
        int start = optionPage * OPTIONS_PER_PAGE;
        int end = Math.min(editableOptions.size(), start + OPTIONS_PER_PAGE);
        int y = 82;
        for (int i = start; i < end; i++) {
            ShaderOptionParser.Option option = editableOptions.get(i);
            String current = currentValue(option);
            Button button = Button.builder(Component.literal(option.id() + ": " + current),
                    ignored -> cycle(option)).bounds(center - 150, y, 300, 20).build();
            addRenderableWidget(button);
            y += 23;
        }
        int navY = height - 55;
        Button previous = Button.builder(Component.literal("<"), ignored -> changeOptionPage(-1))
                .bounds(center - 150, navY, 70, 20).build();
        previous.active = optionPage > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal(">"), ignored -> changeOptionPage(1))
                .bounds(center + 80, navY, 70, 20).build();
        next.active = end < editableOptions.size();
        addRenderableWidget(next);
        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.done"),
                ignored -> onClose()).bounds(center - 60, height - 29, 120, 20).build());
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
        if (next.equals(option.defaultValue())) {
            values.remove(option.id());
            if (ShaderPackOptionStore.save(storePath, packName, option.id(), null)) {
                saveStatus = "Saved (default)";
            } else saveStatus = "Save failed";
        } else if (ShaderPackOptionStore.save(storePath, packName, option.id(), next)) {
            values.put(option.id(), next);
            saveStatus = "Saved";
        } else {
            saveStatus = "Save failed";
        }
        rebuildWidgets();
    }

    private void changeOptionPage(int delta) {
        int pageCount = Math.max(1, (editableOptions.size() + OPTIONS_PER_PAGE - 1) / OPTIONS_PER_PAGE);
        optionPage = Math.max(0, Math.min(pageCount - 1, optionPage + delta));
        rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, rows.size() - visibleRows());
        scrollOffset = (int) Math.max(0, Math.min(max, scrollOffset - Math.signum(delta) * 3));
        return true;
    }

    private int visibleRows() { return Math.max(1, (height - 160) / 12); }

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
        if (!screenDefinition.isBlank()) {
            String declaration = screenDefinition.length() > 100
                    ? screenDefinition.substring(0, 97) + "..." : screenDefinition;
            graphics.drawCenteredString(font, Component.literal(declaration), width / 2, 57, 0xB8B8B8);
        }
        int pageCount = Math.max(1, (editableOptions.size() + OPTIONS_PER_PAGE - 1) / OPTIONS_PER_PAGE);
        graphics.drawCenteredString(font,
                Component.literal("Options " + (editableOptions.isEmpty() ? 0 : optionPage + 1)
                        + "/" + pageCount + "  •  " + editableOptions.size() + " editable"),
                width / 2, height - 68, 0xAAAAAA);
        if (!saveStatus.isEmpty()) graphics.drawCenteredString(font, Component.literal(saveStatus),
                width / 2, height - 82, saveStatus.equals("Save failed") ? 0xFF7777 : 0xA6D8A8);

        int top = Math.max(112, height - visibleRows() * 12 - 88);
        int end = Math.min(rows.size(), scrollOffset + visibleRows());
        for (int i = scrollOffset; i < end; i++) {
            DisplayRow row = rows.get(i);
            graphics.drawCenteredString(font, Component.literal(row.label()), width / 2,
                    top + (i - scrollOffset) * 12, row.heading() ? 0x80D8FF : 0xBBBBBB);
        }
        if (rows.size() > visibleRows()) {
            graphics.drawCenteredString(font, Component.literal("Metadata list — scroll to inspect"),
                    width / 2, height - 96, 0x777777);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
