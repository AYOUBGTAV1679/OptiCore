package dev.opticore.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Read-only bridge to the vanilla chunk compiler's completed layer buffers.
 *
 * <p>The target is intentionally named as a string because CompileResults is
 * package-private. This accessor does not retain or release buffers. A future
 * capture hook must copy each buffer synchronously before vanilla consumes it.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask$CompileResults")
public interface ChunkCompileResultsAccessor {
    @Accessor("renderedLayers")
    Map<RenderType, BufferBuilder.RenderedBuffer> opticore$getRenderedLayers();
}
