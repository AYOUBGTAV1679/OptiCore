# Mixin Renderer Integration — Foundation and Safety Notes

## What changed

The Gradle build now configures SpongePowered MixinGradle with the Mixin 0.8.5 annotation processor, a generated reference map named `opticore.refmap.json`, and the `opticore.mixins.json` configuration. The configuration registers a client-only diagnostic Mixin, `LevelRendererDiagnosticsMixin`. It observes entry into `LevelRenderer.renderChunkLayer(...)` and periodically logs a bounded call count. It does not alter matrices, buffers, shaders, or draw calls.

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

The earlier successful workflow run `45406256000` validated the pre-instrumentation build only; it does **not** validate the active diagnostic Mixin added later. The current branch registers `LevelRendererDiagnosticsMixin` under the client-only `client` list, and the static CI check verifies the JSON registration, expected method descriptor, injection point, `require = 1`, and diagnostic counter. GitHub Actions runs for the current head `3bcd700ffb623556a502f7702176494ae32ce0c6` were queued at the last check; a successful Forge build for that exact head has not yet been confirmed. A real Forge client launch and in-game verification are still pending. No renderer behavior, shadow rendering, or FPS improvement is claimed.

## References

- Forge 47.2.0 `ChunkRenderDispatcher`: https://mcstreetguy.github.io/ForgeJavaDocs/1.20.1-47.2.0/net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.html
- Forge 47.2.0 `RenderChunk`: https://mcstreetguy.github.io/ForgeJavaDocs/1.20.1-47.2.0/net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.RenderChunk.html
- Forge 47.2.0 `RebuildTask`: https://lexxie.dev/forge/1.20.1/net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.RenderChunk.RebuildTask.html
- SpongePowered Mixin on Forge: https://github.com/SpongePowered/Mixin/wiki/Mixins-on-Minecraft-Forge


## Shadow-render hook checkpoint — Forge 47.2.0

The Forge 47.2.0 LevelRenderer API documentation confirms this private method signature:

`renderChunkLayer(RenderType, PoseStack, double, double, double, Matrix4f)`

It is invoked as part of the normal world-layer drawing path, but its private visibility prevents a direct call from OptiCore. A client Mixin could observe the method boundary. The source now declares the mapped JVM descriptor and a diagnostic injection at method HEAD. Static checks validate the source/configuration text, but only the Forge build and a live client launch can confirm that Mixin resolves and applies the target on each supported Forge version.

### Instrumentation acceptance criteria

A first shadow-path experiment must be diagnostic-only and disabled by default:

- Count entry/exit per RenderType on the render thread; do not change matrices, shaders, framebuffer, or draw calls.
- Avoid logging every frame; expose bounded counters or periodic summaries.
- Confirm each expected opaque/cutout layer is observed once in a normal frame and check behavior when the world is paused, unloaded, or resized.
- Confirm the mixin applies on all supported Forge CI versions before considering a shadow draw.

A later depth-only pass must not blindly replay translucent layers as opaque casters. It will need a separate caster-layer policy, light-space matrices, depth state, and complete GL-state restoration. Entity and block-entity casters remain separate work.

**Status (2026-11-25):** A diagnostic-only client Mixin is now registered and targets the documented `renderChunkLayer(RenderType, PoseStack, double, double, double, Matrix4f)` descriptor with `require = 1`. It increments an atomic counter and logs the first observation, then every 1000 calls. This is source-level implementation only: CI runs for commit `525a7d65ecac256b8508439ef41ed370f5a406fe` have been queued, but no successful result for that commit has been confirmed yet. The client has not been launched, so injection application and observed layer frequency remain unverified. No terrain depth pass, entity caster path, shadow map geometry, or functional shadows are implemented. Do not proceed to duplicate/replay the layer draw until the diagnostic injection is proven in a live client.
