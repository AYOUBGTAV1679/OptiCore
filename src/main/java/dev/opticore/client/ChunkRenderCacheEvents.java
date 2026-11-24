package dev.opticore.client;

import dev.opticore.OptiCore;
import net.minecraft.world.level.Level;
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
        if (event.getLevel() instanceof Level level && level.isClientSide()) {
            ChunkRenderCache.invalidateChunk(level,
                    event.getChunk().getPos().x, event.getChunk().getPos().z);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && level.isClientSide()) {
            ChunkRenderCache.invalidateChunk(level,
                    event.getPos().getX() >> 4, event.getPos().getZ() >> 4);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level && level.isClientSide()) {
            ChunkRenderCache.invalidateChunk(level,
                    event.getPos().getX() >> 4, event.getPos().getZ() >> 4);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && level.isClientSide()) {
            ChunkRenderCache.invalidateDimension(level.dimension());
        }
    }
}
