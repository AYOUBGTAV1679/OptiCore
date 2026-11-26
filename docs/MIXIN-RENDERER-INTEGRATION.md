# Mixin Renderer Integration — Foundation and Safety Notes

## Active client mixins

The client mixin configuration registers these renderer-related components:

- `LevelRendererDiagnosticsMixin`: observes `LevelRenderer.renderChunkLayer(...)` and maintains a bounded atomic call counter. It does not alter matrices, buffers, shaders, or draw calls.
- `ChunkRebuildCaptureMixin`: observes the return from the vanilla chunk-section compile method while its `RenderedBuffer` layer payloads are still available. It resolves the region's center chunk through accessors, checks that the center RenderChunk origin agrees with the region coordinates, and skips uncertain layouts.
- `ChunkCompileResultsAccessor`, `RebuildTaskRegionAccessor`, and `RenderChunkRegionAccessor`: expose only the fields needed by that observer.

The rebuild observer does not retain vanilla `RenderedBuffer`, `BufferBuilder`, or GPU `VertexBuffer` objects. When the user enables **Chunk Diagnostics**, it makes bounded defensive CPU copies for observation counters and discards those snapshots. The observation path is disabled when diagnostics are off. The HUD reports observed buffer count, copied bytes, and skipped buffers.

## Why the mesh cache is not connected yet

The current cache is a bounded, thread-safe LRU store for immutable CPU artifacts. The project has a defensive `ImmutableMeshSnapshot` format and a copier for vanilla rendered buffers, but the active rebuild observer does not insert snapshots into the cache and the renderer does not read cached snapshots back.

A real mesh-reuse implementation must first define and test:

1. Complete cache identity for block states, model data, neighbor geometry, lighting, resource generation, render settings, and render-layer behavior.
2. A safe copy/serialization boundary before vanilla releases builder-owned data.
3. Correct reconstruction into a fresh upload payload, without retaining live builder or GPU objects.
4. A cache-read path that can safely replace or bypass vanilla compilation without breaking cancellation, task completion, upload scheduling, or render-thread ownership.
5. Correct invalidation for block edits, neighboring chunk borders, chunk unloads, dimension unloads, resource reloads, and stale asynchronous tasks.
6. A benchmark proving that copying, hashing, storing, and reconstructing meshes costs less than compiling them on target hardware.

The current observer is instrumentation only. It is not a mesh-cache hit path and does not reduce chunk rebuild work. Connecting cache reads/writes before the full dependency and lifecycle contract is proven could cause stale geometry, worker-thread races, use-after-release bugs, or third-party renderer regressions.

## Shadow-rendering boundary

The repository contains shadow-camera matrix helpers, a depth-only framebuffer resource, and a guarded depth-pass executor. These are reusable foundations only. There is no verified caster enumeration for Minecraft terrain, entities, or block entities; no light-space world redraw; and no shadow-map sampling in the active fullscreen pass. The diagnostic `renderChunkLayer` Mixin must not be repurposed to replay arbitrary vanilla layers into a shadow target without a tested caster-layer policy and complete GL-state handling.

## Build validation

The branch's GitHub Actions workflow builds the mod and runs the standalone regression suite across the configured Forge 1.20.1 matrix (47.0.0, 47.2.0, and 47.4.29). The latest verified branch commit at the time of this update is `961629bcae682561eaa13af1bad3816921e05d4d`; all three Forge 1.20.1 jobs completed successfully, including the packaged Mixin registration checks.

This proves source compilation and the automated checks only. A live Minecraft client launch is still required to prove that each Mixin applies on the actual runtime mappings and that observed counts match expected behavior. No runtime performance gain, cache reuse, or functional shadow rendering is claimed.

## References

- Forge 47.2.0 `ChunkRenderDispatcher`: https://mcstreetguy.github.io/ForgeJavaDocs/1.20.1-47.2.0/net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.html
- Forge 47.2.0 `RenderChunk`: https://mcstreetguy.github.io/ForgeJavaDocs/1.20.1-47.2.0/net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.RenderChunk.html
- Forge 47.2.0 `RebuildTask`: https://lexxie.dev/forge/1.20.1/net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.RenderChunk.RebuildTask.html
- SpongePowered Mixin on Forge: https://github.com/SpongePowered/Mixin/wiki/Mixins-on-Minecraft-Forge
