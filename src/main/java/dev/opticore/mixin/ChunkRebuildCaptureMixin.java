package dev.opticore.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.opticore.client.ChunkMeshCapture;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Copies completed CPU-side chunk layer buffers at the return of vanilla's
 * compile method, before RebuildTask.doTask consumes/uploads/releases them.
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

        Level level = ((RenderChunkRegionAccessor) (Object) region).opticore$getLevel();
        if (level == null || !level.isClientSide()) return;

        ChunkRenderDispatcher.RenderChunk renderChunk =
                ((RebuildTaskOuterAccessor) this).opticore$getRenderChunk();
        BlockPos origin = renderChunk.getOrigin();
        Map<net.minecraft.client.renderer.RenderType, BufferBuilder.RenderedBuffer> layers =
                ((ChunkCompileResultsAccessor) result).opticore$getRenderedLayers();

        ChunkMeshCapture.observeCompilation(level, origin, layers);
    }
}
