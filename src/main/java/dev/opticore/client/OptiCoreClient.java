package dev.opticore.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.opticore.OptiCore;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = OptiCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OptiCoreClient {
    public static final String KEY_CATEGORY = "key.categories.opticore";
    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.opticore.open_settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, KEY_CATEGORY);
    public static final KeyMapping ZOOM = new KeyMapping(
            "key.opticore.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, KEY_CATEGORY);

    private OptiCoreClient() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
        event.register(ZOOM);
    }
}
