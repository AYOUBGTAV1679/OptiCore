package dev.opticore.client;

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
    }

    @Override
    public String getName() {
        return "OptiCore render cache invalidation";
    }
}
