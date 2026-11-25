# OptiCore Alpha 07 — Engineering Status

## Scope

Alpha 07 began as a cache-hardening milestone. The branch now also contains experimental shader-pack inspection, render-graph, scene-uniform, OpenGL resource, and directional-shadow foundations. It is still not a verified Minecraft renderer or OptiFine-compatible shader implementation.

## Implemented foundations

- Bounded, thread-safe LRU cache with explicit invalidation and standalone regression coverage.
- Shader-pack discovery and bounded archive/source inspection.
- GLSL stage indexing, include expansion, interface scanning, and preflight diagnostics.
- Explicit render-thread shader compile/link and bounded program cache.
- Depth/color render-target resource management and resource-reload cleanup.
- Declarative render graph with validation and a backend-agnostic executor.
- Immutable scene and environment snapshots with matrix/uniform helpers.
- Shadow depth target and an explicitly invoked depth-only pass primitive.
- Directional shadow camera matrix builder with orthographic projection and texel snapping to reduce sub-texel shadow movement.

## Shadow implementation boundary

The directional camera builder accepts a light direction supplied by a caller. It does not currently derive the actual Minecraft sun/moon direction, track weather/time transitions, fit a camera-frustum cascade, or enumerate terrain/entity casters.

The ShadowPass accepts a caller-provided caster renderer. It does not hook into Forge's world-render stages, render Minecraft terrain/entities, bind a complete pack-compatible shadow program, expose the depth texture to the world shader, or composite shadows into the final frame. Therefore visible in-game shadows are not implemented yet.

The scene capture currently observes AFTER_LEVEL; it does not provide the correct earlier world-render hook for a shadow pass. No shadow feature should be advertised as active in gameplay until the entire path is connected and tested.

## Validation

- The standalone regression script covers cache, shader inspection, matrix, scene snapshot, and render-graph logic.
- The shadow camera math has dependency-free tests for view/projection construction, vertical light directions, defensive copies, input validation, and sub-texel stability.
- Forge build and test results are reported by GitHub Actions for the exact commit. A successful compile does not prove that OpenGL resources or rendered output work in a live Minecraft client.
- No controlled FPS benchmark, shader-pack compatibility certification, or in-game shadow visual test has been completed.

## Not implemented / not claimed

- Actual compiled chunk-mesh caching or a verified FPS gain.
- A full independent world renderer.
- End-to-end OptiFine shader-pack compatibility.
- Live terrain/entity shadow rendering or shadow-map sampling in the world shader.
- Complete fog, lightmap, entity, block-entity, hand, water, reflection, and post-processing pipelines.
- Runtime compatibility certification with third-party mods.

## Next milestone

Integrate a single shadow pass at a verified Forge 1.20.1 world-render stage, derive the directional light from the actual level time/weather state, and render a deliberately limited set of casters first. Then sample the resulting depth texture in a minimal world shader and verify state restoration and visual output in a real Forge client before expanding to entities, cascades, or shader-pack compatibility.
