# Renderer Cache Lifecycle Bridge

This subsystem provides bounded storage and conservative invalidation for future
CPU-side render artifacts.

## Implemented
- Client-side cache owner with a configurable memory budget (default 32 MiB).
- Chunk invalidation when a client chunk unloads.
- Block-edit invalidation for the edited chunk and horizontal neighbors at chunk borders.
- Dimension invalidation when a client level unloads.
- Resource reload invalidation (including resource-pack reloads).
- Cache hit/miss and memory-use diagnostics in the OptiCore HUD.
- Extended immutable cache-key shape with dependency fingerprints for neighbor
  block-state geometry, lighting, and render settings.
- Regression tests verify that changing each dependency fingerprint produces a
  distinct cache identity.

## Critical limitation
This is **not compiled-mesh caching** and does not claim an FPS improvement.
There is still no verified hook into Minecraft's chunk mesh compilation path.
No compiled section geometry is currently stored or reused, so cache hit/miss
counters remain zero during normal gameplay. The new fingerprinted key is
infrastructure for a future mesh producer, not proof that those fingerprints
are already sampled from the live world.

No live `RenderChunk`, world object, or GPU buffer is retained. GPU buffers must
continue to follow Minecraft's own upload and disposal lifecycle.

## Gate before enabling mesh reuse
A future producer must compute stable fingerprints from all inputs that affect
the compiled section: neighbor block states, block and sky light, resource
generation, render settings, and any additional geometry-affecting state. It
must then be connected at a verified CPU mesh compilation boundary. Until the
integration passes tests for edits, border changes, lighting, reloads, dimension
changes, chunk unloads, and memory pressure, OptiCore must not skip vanilla
mesh compilation or reuse a cached artifact.
