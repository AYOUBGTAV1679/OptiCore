package dev.opticore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Diagnostic-only observation of vanilla chunk-layer rendering.
 * Does not alter matrices, buffers, shaders, or draw calls.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererDiagnosticsMixin {
    @Unique
    private static volatile long opticore$chunkLayerEntries;

    @Inject(
            method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
            at = @At("HEAD"),
            require = 0
    )
    private void opticore$observeChunkLayer(
            RenderType renderType,
            PoseStack poseStack,
            double x,
            double y,
            double z,
            Matrix4f projection,
            CallbackInfo ci
    ) {
        opticore$chunkLayerEntries++;
    }

    /** Diagnostic counter; zero means the injection has not been observed. */
    public static long opticore$getChunkLayerEntries() {
        return opticore$chunkLayerEntries;
    }
}
