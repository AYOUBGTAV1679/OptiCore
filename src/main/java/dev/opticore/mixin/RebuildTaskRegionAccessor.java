package dev.opticore.mixin;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only access to the region snapshot owned by a vanilla rebuild task. */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public interface RebuildTaskRegionAccessor {
    @Accessor("region")
    RenderChunkRegion opticore$getRegion();
}
