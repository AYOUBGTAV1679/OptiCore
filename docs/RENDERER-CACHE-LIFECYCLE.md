# Renderer Cache Lifecycle Bridge

This change connects the existing bounded LRU cache to client-world lifecycle events.

## Implemented
- A client renderer cache owner with a 32 MiB initial budget.
- Chunk invalidation on client chunk unload.
- Chunk invalidation on block break and placement.
- Dimension invalidation when a client level unloads.
- A global invalidation method for future resource reload/settings hooks.

## Important limitation
This is **not yet compiled-mesh caching** and does not claim an FPS improvement. No live
RenderChunk, world object, or GPU buffer is retained. The cache currently exposes a safe
storage/lifecycle API for disposable CPU-side render artifacts.

The next required step is a verified hook at the chunk mesh compilation boundary. That
hook must capture immutable CPU-side build output, include block/neighbour/light/resource/
render-setting revisions in its key, and prove correct invalidation before any cached
artifact is reused. GPU buffers must continue to follow Minecraft's upload/disposal
lifecycle and must not be retained by this generic cache.
