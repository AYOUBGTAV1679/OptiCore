package dev.opticore.client;

import dev.opticore.OptiCore;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registers OptiCore's client-only resource reload listener. */
@Mod.EventBusSubscriber(modid = OptiCore.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OptiCoreClientReloadRegistration {
    private OptiCoreClientReloadRegistration() {}

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new OptiCoreResourceReloadListener());
    }
}
