package dev.opticore.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.opticore.client.ChunkMeshCapture;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.lang.reflect.Field;

/**
 * Observes completed CPU-side chunk layer buffers before vanilla consumes them.
 * The center cell is accepted only when its origin agrees with the region's
 * explicit center chunk coordinates; uncertain region layouts are skipped.
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

        ChunkRenderDispatcher.RenderChunk[][] chunks = opticore$readChunkGrid(region);
        if (chunks == null || chunks.length == 0) return;
        ChunkRenderDispatcher.RenderChunk[] centerRow = chunks[chunks.length / 2];
        if (centerRow == null || centerRow.length == 0) return;
        ChunkRenderDispatcher.RenderChunk target = centerRow[centerRow.length / 2];
        if (target == null) return;

        BlockPos origin = target.getOrigin();
        int chunkX = origin.getX() >> 4;
        int sectionY = origin.getY() >> 4;
        int chunkZ = origin.getZ() >> 4;

        // RenderChunkRegion stores the center chunk coordinates independently
        // of the backing grid. Refuse to attribute buffers if they disagree.
        if (chunkX != regionAccessor.opticore$getCenterX()
                || chunkZ != regionAccessor.opticore$getCenterZ()) {
            return;
        }

        Map<net.minecraft.client.renderer.RenderType, BufferBuilder.RenderedBuffer> layers =
                ((ChunkCompileResultsAccessor) result).opticore$getRenderedLayers();
        ChunkMeshCapture.observeCompilation(level, chunkX, chunkZ, sectionY, layers);
    }
    /**
     * Forge 1.20.1 exposes this protected grid in source, but the Mixin
     * annotation processor cannot resolve its field mapping consistently.
     * Resolve only this diagnostic field at runtime using known mapping aliases.
     */
    private static ChunkRenderDispatcher.RenderChunk[][] opticore$readChunkGrid(
            RenderChunkRegion region) {
        for (String name : new String[]{"chunks", "f_112905_", "a"}) {
            try {
                Field field = RenderChunkRegion.class.getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(region);
                if (value instanceof ChunkRenderDispatcher.RenderChunk[][] grid) return grid;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Fail closed if this Minecraft/Forge mapping is unexpected.
            }
        }
        return null;
    }

}
