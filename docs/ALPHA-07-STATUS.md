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
- Directional shadow camera matrix builder with orthographic projection, texel snapping, and float-range validation.
- Immutable capture of Minecraft's interpolated `Level#getSunAngle(partialTick)` plus world time and weather at `AFTER_LEVEL`.

## Shadow implementation boundary

The directional camera builder still accepts a light direction supplied by a caller. The client now captures vanilla's interpolated sun angle and weather/time values at `AFTER_LEVEL`, but does not yet convert the vanilla celestial transform into a validated world-space light vector, fit a camera-frustum cascade, or enumerate terrain/entity casters.

The ShadowPass accepts a caller-provided caster renderer. It does not hook into Forge's world-render stages, render Minecraft terrain/entities, bind a complete pack-compatible shadow program, expose the depth texture to the world shader, or composite shadows into the final frame. Therefore visible in-game shadows are not implemented yet.

The scene capture currently observes AFTER_LEVEL; it does not provide the correct earlier world-render hook for a shadow pass. No shadow feature should be advertised as active in gameplay until the entire path is connected and tested.

## Validation

- The standalone regression script covers cache, shader inspection, matrix, scene snapshot, directional sun snapshot validation, and render-graph logic.
- The shadow camera math has dependency-free tests for view/projection construction, vertical light directions, defensive copies, input validation, and sub-texel stability.
- Forge build and test results are reported by GitHub Actions for the exact commit. A successful compile does not prove that OpenGL resources or rendered output work in a live Minecraft client.
- No controlled FPS benchmark, shader-pack compatibility certification, or in-game shadow visual test has been completed.

## Renderer Mixin verification checkpoint

A client-only diagnostic Mixin is registered for `LevelRenderer.renderChunkLayer(...)`. It increments a counter and emits a bounded log message; it does not modify the render pipeline. CI checks the source configuration and verifies the packaged mod JAR contains the Mixin config and refmap. A prior validation run completed successful Forge builds and packaged-Mixin checks for Forge 47.0.0, 47.2.0, and 47.4.29 on commit `ef3c6028e681ce14b35296cc0606177501d10aea`. Subsequent commits add regression tests for the packaged-JAR checker; their workflow results must be checked separately. Even a successful build does not prove the injection applies in-game: a live Forge 47.2.0 launch is still required to record actual render-layer calls.

Do not implement a shadow draw by replaying vanilla chunk layers from this diagnostic hook. First verify the hook at runtime, then identify a supported world-render integration point and implement a dedicated depth-only caster path with explicit opaque/cutout policy and GL-state restoration.

## Not implemented / not claimed

- Actual compiled chunk-mesh caching or a verified FPS gain.
- A full independent world renderer.
- End-to-end OptiFine shader-pack compatibility.
- Live terrain/entity shadow rendering or shadow-map sampling in the world shader.
- Complete fog, lightmap, entity, block-entity, hand, water, reflection, and post-processing pipelines.
- Runtime compatibility certification with third-party mods.


## Client UI status

- The main settings screen has a two-column layout with the currently implemented zoom, FPS, frame-time, cache-budget, diagnostics, scene-capture, compatibility, and shader-pack controls.
- The shader-pack manager supports paginated discovery, selection, refresh, and opening the shaderpacks directory. It returns to the page containing the active selection when opened.
- The shader options screen is intentionally read-only. It previews the parsed `screen` declaration and discovered option identifiers with scrolling; it does not create editable toggles/sliders or apply overrides to shader programs.
- UI labels for the current shader option preview are localized in English and Arabic. The layouts have passed source/build checks where Actions reports success, but still require visual verification in a live Minecraft client at multiple GUI scales and window sizes.

## Next milestone

Integrate a single shadow pass at a verified Forge 1.20.1 world-render stage, derive the directional light from the actual level time/weather state, and render a deliberately limited set of casters first. Then sample the resulting depth texture in a minimal world shader and verify state restoration and visual output in a real Forge client before expanding to entities, cascades, or shader-pack compatibility.
