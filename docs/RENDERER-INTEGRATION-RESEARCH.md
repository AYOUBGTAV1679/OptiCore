# Renderer Integration Research — Minecraft 1.20.1 / Forge 47.2.0

## Findings

The vanilla client chunk renderer is not a simple cache lookup. It schedules asynchronous compilation work, builds per-layer vertex data, and uploads rendered buffers for later drawing.

Relevant 1.20.1 Forge API surface:

- `net.minecraft.client.renderer.chunk.ChunkRenderDispatcher` owns high- and low-priority compile queues, worker execution, pending uploads, and upload scheduling.
- `ChunkRenderDispatcher.RenderChunk` owns an atomic reference to its `CompiledChunk`, per-`RenderType` `VertexBuffer` objects, dirty state, and rebuild/resort tasks.
- `LevelRenderer` exposes `getChunkRenderDispatcher()`, chunk statistics, and rendered-chunk counters.
- Forge's `RenderLevelStageEvent` is a world-render stage event. It is useful for diagnostics or additional draw passes, but it is not a hook into chunk mesh compilation or cache reuse.

Reference API documentation:
- Forge 47.2.0 ChunkRenderDispatcher: https://mcstreetguy.github.io/ForgeJavaDocs/1.20.1-47.2.0/net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.html
- Forge 47.2.0 RenderChunk: https://mcstreetguy.github.io/ForgeJavaDocs/1.20.1-47.2.0/net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.RenderChunk.html
- Forge 47.2.0 LevelRenderer: https://mcstreetguy.github.io/ForgeJavaDocs/1.20.1-47.2.0/net/minecraft/client/renderer/LevelRenderer.html

## Why the existing generic cache cannot simply hold compiled chunks

A compiled render result is coupled to more than chunk coordinates. Its validity can depend on block states, neighboring sections, lighting, render layers, resource/model state, and transparency sorting. GPU vertex buffers also have explicit allocation, upload, replacement, and release lifecycles, and must not be reused after their owning render resources are invalidated.

Therefore, using only (dimension, chunk X, chunk Z) as a key and storing live GPU buffers would risk stale geometry, incorrect lighting, resource leaks, and crashes.

## Safe integration direction

1. Add diagnostics first: capture dispatcher queue/upload counts and existing chunk statistics at a low frequency, without changing scheduling.
2. Profile a repeatable scene and record baseline frame time, chunk rebuild activity, upload queue behavior, and memory use.
3. Identify an interception point for CPU-side immutable mesh/build output before GPU upload. Avoid caching live `VertexBuffer` instances.
4. Define a complete invalidation key covering world/session identity, section position, relevant neighbor/state revisions, resource reload generation, and render configuration.
5. Prototype behind a disabled-by-default experimental flag and compare output against vanilla after block edits, light updates, resource reloads, dimension changes, and chunk unloads.
6. Only then evaluate whether caching reduces rebuild cost without increasing memory or causing stale geometry.

## Current project status

This research is a design note, not an implementation. OptiCore currently has a generic bounded cache and client UI/configuration. It does not yet intercept the vanilla chunk compilation pipeline, replace the renderer, or demonstrate an FPS improvement.

A ForgeGradle build and in-game test are still required before any new integration code can be described as verified.
