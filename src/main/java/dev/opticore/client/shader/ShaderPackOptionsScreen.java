package dev.opticore.client.shader;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Read-only preview of option identifiers declared by a shader pack. */
public final class ShaderPackOptionsScreen extends Screen {
    private final Screen parent;
    private final String packName;
    private final List<String> controls;
    private int scrollOffset;

    public ShaderPackOptionsScreen(Screen parent, String packName,
                                   ShaderPackMetadata.Summary metadata) {
        super(Component.translatable("screen.opticore.shaders.options_title"));
        this.parent = parent;
        this.packName = packName;
        this.controls = metadata.controls();
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.opticore.done"),
                button -> onClose()).bounds(width / 2 - 60, height - 30, 120, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, controls.size() - Math.max(1, (height - 110) / 18));
        scrollOffset = (int) Math.max(0, Math.min(max, scrollOffset - Math.signum(delta) * 3));
        return true;
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
        graphics.drawCenteredString(font, Component.literal(packName), width / 2, 32, 0xC8E6FF);
        graphics.drawCenteredString(font,
                Component.translatable("screen.opticore.shaders.options_readonly"),
                width / 2, 49, 0xE0C080);

        int visible = Math.max(1, (height - 110) / 18);
        int end = Math.min(controls.size(), scrollOffset + visible);
        if (controls.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.opticore.shaders.options_empty"),
                    width / 2, height / 2, 0xAAAAAA);
        } else {
            for (int i = scrollOffset; i < end; i++) {
                graphics.drawCenteredString(font, Component.literal(controls.get(i)),
                        width / 2, 70 + (i - scrollOffset) * 18, 0xFFFFFF);
            }
        }
        graphics.drawCenteredString(font,
                Component.translatable("screen.opticore.shaders.options_count", controls.size()),
                width / 2, height - 48, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
