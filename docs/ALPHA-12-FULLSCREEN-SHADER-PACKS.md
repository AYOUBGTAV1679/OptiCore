# Alpha 12 — guarded fullscreen shader-pack execution

## What is implemented

The client now has a narrow, real shader-pack execution path for the fullscreen scene-composite pass:

1. The selected folder or ZIP pack is discovered through the existing bounded repository.
2. Paired vertex/fragment sources are assembled with bounded include resolution and preflight diagnostics.
3. Recognized per-pack option overrides are applied to the assembled GLSL before compilation.
4. `FullscreenShaderContract` rejects programs that do not match the fullscreen interface used by this pass.
5. The OpenGL driver compiles and links the selected candidate on the render thread.
6. Linked uniform locations and active OpenGL uniform types are checked before the program is accepted.
7. The accepted program is supplied to the fullscreen composite. The program cache owns its lifetime.
8. A pack selection, option edit, or resource reload invalidates the active binding so the next world frame resolves the current program.

The accepted interface is intentionally exact:

- Vertex stage generates a fullscreen triangle from `gl_VertexID`.
- No vertex attributes.
- Vertex output and fragment input: `vec2 vUv`.
- Fragment output: `vec4 fragColor`.
- Fragment uniforms:
  - `sampler2D uSceneColor`
  - `sampler2D uSceneDepth`
  - `float uWarmth`
  - `float uFogStrength`
  - `mat4 uInverseProjection`

The linked GL program must keep all five uniforms active with the expected types. A source-level declaration by itself is not sufficient.

## Render graph and failure behavior

The world path runs at Forge's `AFTER_LEVEL` stage only when `experimentalWorldPass` is enabled. Its graph has two ordered passes:

- `capture`: reads the vanilla scene and writes isolated scene-color and scene-depth resources.
- `composite`: reads those captured resources and writes the composite output to the vanilla target.

The graph executor validates the graph and resolves all handlers before executing. If capture or composite fails after capture, OptiCore attempts to restore the original captured scene to the vanilla target. The world pass remains opt-in through the **Experimental World Pass** toggle in OptiCore settings, and the vanilla path remains the default.

If the selected pack has no program satisfying this contract, OptiCore records a diagnostic and uses its built-in fullscreen composite rather than passing an arbitrary pack shader into the renderer.

## What this does not mean

This is not general OptiFine shader-pack support. It does not yet implement:

- terrain and entity shader stages;
- OptiFine's multi-program world pipeline or multiple render targets;
- shadow caster rendering, shadow filtering, or shadow-map sampling;
- water, reflections, translucent-material handling, or custom lighting models;
- complete OptiFine uniform, texture, lightmap, and material compatibility;
- arbitrary shader-pack vertex layouts or additional uniforms.

Many existing packs will therefore be rejected by this narrow fullscreen contract. The source preflight count shown in the pack screen is not a compatibility percentage.

## Validation

The standalone shader-contract checks and all three configured Forge 1.20.1 CI build variants must pass. CI success proves compilation and the included automated checks only; it does not prove the pass works on every GPU or in a live Minecraft session.

Before enabling this option in a normal play instance, test on a disposable world and record the GPU/driver, graphics mode, pack name/version, and log output. Test world entry/exit, window resize, resource reload, pack switch, option edits, and a forced shader compile failure. Fabulous graphics mode requires separate validation.

## Current boundary

The alpha now has a guarded fullscreen pack-program path, not a completed renderer. Dynamic shadows, water/reflections, terrain/entity rendering, actual mesh-cache reuse, broad shader compatibility, and controlled performance benchmarks remain separate unfinished work.
