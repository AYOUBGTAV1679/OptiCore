package dev.opticore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Diagnostic-only observation of vanilla chunk-layer rendering.
 * Does not alter matrices, buffers, shaders, or draw calls.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererDiagnosticsMixin {
    @Unique
    private static final Logger OPTICORE$LOGGER = LogUtils.getLogger();

    @Unique
    private static final AtomicLong OPTICORE$CHUNK_LAYER_ENTRIES = new AtomicLong();

    @Inject(
            method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
            at = @At("HEAD"),
            require = 1
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
        long count = OPTICORE$CHUNK_LAYER_ENTRIES.incrementAndGet();
        // One log line per thousand calls avoids flooding the client log.
        if (count == 1L || count % 1000L == 0L) {
            OPTICORE$LOGGER.info("[OptiCore] Observed {} chunk-layer render calls; latest layer: {}",
                    count, renderType);
        }
    }
}
