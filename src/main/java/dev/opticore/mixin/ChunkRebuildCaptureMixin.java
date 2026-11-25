package dev.opticore.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.opticore.client.ChunkMeshCapture;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Observes completed CPU-side chunk layer buffers before vanilla consumes them.
 * The region supplies horizontal chunk coordinates; section identity is not
 * available through this task's stable public surface, so observations are
 * deliberately discarded and never inserted into the reusable mesh cache.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public abstract class ChunkRebuildCaptureMixin {
    @Inject(
            method = "compile(FFFLnet/minecraft/client/renderer/ChunkBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk$RebuildTask$CompileResults;",
            at = @At("RETURN"),
            require = 1
    )
    private void opticore$captureCompletedMeshes(
            float cameraX,
            float cameraY,
            float cameraZ,
            ChunkBufferBuilderPack buffers,
            CallbackInfoReturnable<Object> cir
    ) {
        Object result = cir.getReturnValue();
        if (result == null) return;

        RenderChunkRegion region = ((RebuildTaskRegionAccessor) this).opticore$getRegion();
        if (region == null) return;

        RenderChunkRegionAccessor regionAccessor = (RenderChunkRegionAccessor) (Object) region;
        Level level = regionAccessor.opticore$getLevel();
        if (level == null || !level.isClientSide()) return;

        Map<net.minecraft.client.renderer.RenderType, BufferBuilder.RenderedBuffer> layers =
                ((ChunkCompileResultsAccessor) result).opticore$getRenderedLayers();
        ChunkMeshCapture.observeCompilation(
                level,
                regionAccessor.opticore$getCenterX(),
                regionAccessor.opticore$getCenterZ(),
                layers
        );
    }
}
