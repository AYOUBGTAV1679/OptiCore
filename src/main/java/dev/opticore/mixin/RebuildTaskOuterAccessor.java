package dev.opticore.mixin;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses the enclosing RenderChunk instance of its rebuild task. */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public interface RebuildTaskOuterAccessor {
    @Accessor("this$0")
    ChunkRenderDispatcher.RenderChunk opticore$getRenderChunk();
}
