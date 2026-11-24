package dev.opticore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.opticore.client.shader.GlShaderProgramCache;
import dev.opticore.client.shader.GlRenderTargetManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Invalidates derived render artifacts whenever client resources are rebuilt
 * (for example after F3+T or changing resource packs).
 */
public final class OptiCoreResourceReloadListener implements ResourceManagerReloadListener {
    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        ChunkRenderCache.onResourcesReloaded();
        // Resource reload callbacks are not guaranteed to run with the GL
        // context current. Defer program deletion to Minecraft's render queue.
        RenderSystem.recordRenderCall(() -> {
            GlShaderProgramCache.clear();
            GlRenderTargetManager.clear();
        });
    }

    @Override
    public String getName() {
        return "OptiCore render cache invalidation";
    }
}
