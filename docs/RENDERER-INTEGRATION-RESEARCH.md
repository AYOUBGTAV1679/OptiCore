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


## Confirmed compilation boundary (Forge 47.2.0)

The 1.20.1 API documentation exposes a more precise candidate than a render-stage event:

- `RenderChunk.RebuildTask` performs the rebuild.
- Its private `compile(...)` method produces `CompileResults`.
- `CompileResults` contains a `Map<RenderType, BufferBuilder.RenderedBuffer>`, a visibility set, transparency sort state, and block-entity lists.
- `RebuildTask.doTask(...)` is the asynchronous task entry point that invokes the compile work and coordinates the result.

This confirms where CPU-side build output exists before it becomes GPU vertex buffers. It does **not** mean the result is already safe to cache. `RenderedBuffer` is a builder-owned upload payload with a lifecycle, and the transparency sort state plus block-entity/visibility data must remain consistent with the render chunk. A prototype must establish ownership, copying/serialization, cancellation behavior, and the exact point at which the task result is accepted or discarded.

### Candidate hook assessment

1. **After `compile(...)` returns:** the most direct point to observe the completed CPU build result, but the method and nested result type are private/package-scoped implementation details. A Mixin injection here is version-sensitive and must be verified against the actual Forge 47.2.0 bytecode/mappings.
2. **Inside `doTask(...)`:** a possible integration boundary for task lifecycle and cancellation. It still requires careful handling of asynchronous completion and cannot simply retain the returned `RenderedBuffer` objects.
3. **At `uploadChunkLayer(...)`:** too late for a general mesh cache if the only retained object is a live upload buffer; it is useful for upload instrumentation, not proof of reusable immutable mesh data.
4. **At `RenderLevelStageEvent`:** not suitable for compile-result interception.

No Mixin has been added yet. The next implementation step is to inspect the exact Forge 47.2.0 source/bytecode and identify a stable injection point with a testable ownership contract. Do not enable cache reuse until the payload can be retained independently of pooled builders and safely rejected on cancellation or invalidation.


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

The latest Alpha 07 commit has passed the cache regression tests and ForgeGradle build, and produced a downloadable JAR artifact. An in-game test is still required before any renderer integration or performance claim can be described as verified.
