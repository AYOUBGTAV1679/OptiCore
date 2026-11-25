package dev.opticore.mixin;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only access to the world, center coordinates, and chunk grid used by compilation. */
@Mixin(RenderChunkRegion.class)
public interface RenderChunkRegionAccessor {
    @Accessor("level")
    Level opticore$getLevel();

    @Accessor("centerX")
    int opticore$getCenterX();

    @Accessor("centerZ")
    int opticore$getCenterZ();
}
