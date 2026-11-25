package dev.opticore.mixin;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only access to the region snapshot owned by a vanilla rebuild task. */
@Mixin(ChunkRenderDispatcher.RenderChunk.RebuildTask.class)
public interface RebuildTaskRegionAccessor {
    @Accessor("region")
    RenderChunkRegion opticore$getRegion();
}
