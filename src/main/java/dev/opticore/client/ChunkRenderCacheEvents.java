package dev.opticore.client;

import dev.opticore.OptiCore;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps cached render artifacts from surviving world edits or world unloads.
 * This is lifecycle integration only; it does not intercept mesh compilation.
 */
@Mod.EventBusSubscriber(modid = OptiCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ChunkRenderCacheEvents {
    private ChunkRenderCacheEvents() {}

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ChunkRenderCache.invalidateChunk(event.getLevel(),
                    event.getChunk().getPos().x, event.getChunk().getPos().z);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        ChunkRenderCache.invalidateChunk(event.getLevel(),
                event.getPos().getX() >> 4, event.getPos().getZ() >> 4);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        ChunkRenderCache.invalidateChunk(event.getLevel(),
                event.getPos().getX() >> 4, event.getPos().getZ() >> 4);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ChunkRenderCache.invalidateDimension(event.getLevel().dimension());
        }
    }
}
