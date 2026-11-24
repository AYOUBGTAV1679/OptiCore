# Renderer Cache Lifecycle Bridge

This change connects the bounded, Minecraft-independent LRU cache to client-world
lifecycle events so stale derived render artifacts can be discarded safely.

## Implemented
- Client-side cache owner with a configurable memory budget (initially 32 MiB).
- Chunk invalidation when a client chunk unloads.
- Block-edit invalidation for the edited chunk.
- Neighbor-chunk invalidation when a block edit occurs on a horizontal chunk
  boundary (local X/Z coordinate 0 or 15), where adjacent chunk face geometry
  may also become stale.
- Dimension invalidation when a client level unloads.
- Client resource reload invalidation, including resource-pack reloads (F3+T).
- Cache hit/miss and memory-use diagnostics in the OptiCore HUD.

## Important limitation
This is **not compiled-mesh caching** and does not claim an FPS improvement. The
cache currently provides bounded storage and invalidation plumbing; it does not
intercept Minecraft's chunk mesh compiler, store compiled section geometry, or
reuse GPU buffers. The HUD counters therefore remain zero until a future
mesh-producing code path actually stores entries.

No live `RenderChunk`, world object, or GPU buffer is retained. GPU buffers must
continue to follow Minecraft's upload/disposal lifecycle.

## Next engineering gate
Before enabling reuse, implement and verify a hook at the chunk mesh compilation
boundary. Cache keys must account for dimension, chunk/section coordinates,
neighbor state, block and sky light, resource reload generation, render settings,
and any other inputs that affect emitted geometry. Tests must cover block edits
(including chunk borders), lighting updates, resource reloads, dimension changes,
chunk unloads, and memory pressure. Until those tests pass, cache hits must never
be used to skip a vanilla mesh build.
