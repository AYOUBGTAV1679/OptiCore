# Mixin Renderer Integration — Foundation and Safety Notes

## What changed

The Gradle build now configures SpongePowered MixinGradle with the Mixin 0.8.5 annotation processor, a generated reference map named `opticore.refmap.json`, and the `opticore.mixins.json` configuration. The configuration is intentionally empty at this stage: it declares no injections and therefore does not alter Minecraft renderer behavior.

Forge already ships the Mixin runtime. OptiCore does not shade or bundle a second Mixin runtime into its mod JAR.

## Verified Forge 47.2.0 target surface

For Minecraft 1.20.1 / Forge 47.2.0, the documented rebuild path includes:

- `ChunkRenderDispatcher.RenderChunk.RebuildTask.doTask(ChunkBufferBuilderPack)`, which returns a `CompletableFuture<ChunkTaskResult>`.
- A private `RebuildTask.compile(float, float, float, ChunkBufferBuilderPack)` method returning the nested `CompileResults`.
- `CompileResults.renderedLayers`, a map of render layers to `BufferBuilder.RenderedBuffer`.
- `CompileResults.visibilitySet`, `transparencyState`, `blockEntities`, and `globalBlockEntities`.

The public dispatcher also exposes queue/upload counters, but those counters are diagnostics, not a mesh-cache integration point.

## Why the cache hook is not enabled yet

A `RenderedBuffer` is a builder-owned upload payload. Retaining it after the vanilla upload/cleanup lifecycle risks reusing released or pooled memory. It is also not a durable immutable mesh representation that can safely be replayed into a later upload.

A real cache implementation must first define and test:
1. An immutable CPU-side representation for each render layer.
2. A safe copy/serialization boundary before vanilla releases builder-owned data.
3. Correct reconstruction into a fresh upload payload, without retaining live `BufferBuilder` or GPU `VertexBuffer` objects.
4. Complete cache identity for block states, model data, neighbor geometry, lighting, resource generation, render settings, and relevant Forge render-layer behavior.
5. Cancellation, stale-task, chunk unload, dimension unload, and resource-reload behavior.
6. A benchmark proving that copying and reconstructing meshes costs less than recompiling them on target hardware.

Until these conditions are met, injecting a cache read/write into `compile` or `doTask` would risk stale geometry, use-after-release bugs, worker-thread races, or regressions for third-party renderers.

## Next implementation step

Use a minimal client-only instrumentation mixin around the rebuild future to measure compilation completion and cancellation behavior first. Do not retain `RenderedBuffer` instances. After runtime verification on Forge 47.2.0, prototype an owned CPU-side mesh snapshot and test round-trip reconstruction before connecting it to SmartChunkCache.

## Validation status

Mixin build configuration has been added, but the empty configuration has no active mixin classes. A successful Gradle build is still required for this configuration change, followed by a real Forge client launch. No renderer behavior or FPS improvement is claimed by this foundation change.

## References

- Forge 47.2.0 `ChunkRenderDispatcher`: https://mcstreetguy.github.io/ForgeJavaDocs/1.20.1-47.2.0/net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.html
- Forge 47.2.0 `RenderChunk`: https://mcstreetguy.github.io/ForgeJavaDocs/1.20.1-47.2.0/net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.RenderChunk.html
- Forge 47.2.0 `RebuildTask`: https://lexxie.dev/forge/1.20.1/net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.RenderChunk.RebuildTask.html
- SpongePowered Mixin on Forge: https://github.com/SpongePowered/Mixin/wiki/Mixins-on-Minecraft-Forge
