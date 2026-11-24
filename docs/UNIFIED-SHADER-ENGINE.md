# Unified Shader Engine — Architecture and Compatibility Plan

## Product direction

OptiCore will have one integrated shader engine, not separate shader implementations for lighting, shadows, water, or post-processing. A selected shader pack is loaded once and its supported programs are executed through the same render pipeline and resource manager.

The target is practical compatibility with the OptiFine shader-pack format used by Minecraft 1.20.1. This is a compatibility target, not a claim that every OptiFine pack or extension will work.

## Non-negotiable constraints

- Minecraft 1.20.1, Forge 47.2.0, Java 17.
- No Embeddium or Oculus dependency.
- Shader support must be optional; vanilla rendering remains the safe fallback.
- Shader compilation and GPU resource creation/destruction must happen on the correct render thread.
- Never keep stale framebuffer, texture, or program handles across a resource reload or window resize.
- Unsupported directives and failed shader programs must produce actionable log messages instead of silently corrupting rendering.
- Low-memory devices need conservative defaults and a way to disable expensive passes.

## Single-pipeline design

1. **Pack discovery** — enumerate shader-pack folders and ZIP files under the Minecraft shaderpacks directory. Do not extract untrusted archives blindly; validate paths and impose size/count limits.
2. **Pack metadata and options** — read supported shaders.properties values, expose pack options, and preserve unknown options without pretending they are implemented.
3. **Compatibility parser** — parse the supported OptiFine-style program layout, shader properties, uniforms, and texture declarations. Report unsupported syntax before activation.
4. **Program compiler** — compile vertex/fragment programs and link them into owned program objects. Cache compiled results only for the current resource generation and graphics context.
5. **Render graph** — execute supported shadow, terrain/entity, composite, and final passes in a deterministic order. Every pass declares inputs, outputs, depth behavior, and resource dependencies.
6. **Resource manager** — allocate/reuse framebuffers and textures according to resolution scale and memory budget. Recreate them on resize, pack switch, graphics-context change, and resource reload.
7. **Fallback and diagnostics** — if a pack fails validation or compilation, disable the pack cleanly, restore vanilla rendering, and show the exact failing program and log location.

All visual effects (shadows, water, reflections, volumetrics, color grading, and post-processing) must be expressed as passes or shared pipeline features in this one engine. They must not create parallel shader systems.

## Compatibility scope

### Initial compatibility milestone
- Discover folders and ZIP packs.
- Validate archive paths and required shader directory structure.
- Read basic pack metadata and options.
- Parse common GLSL program names and declared texture/uniform references.
- Compile a minimal test program and report compile/link errors.
- Provide a safe enable/disable switch and automatic vanilla fallback.

### Later compatibility milestones
- Shadow map passes and shadow settings.
- Terrain and entity shader programs.
- Composite and final post-processing passes.
- Common OptiFine uniforms, samplers, and shader properties.
- Multiple render targets and depth/color attachments.
- Pack options and per-pack settings persistence.
- Performance profiles (Low, Balanced, High) that adjust resolution and expensive passes without rewriting pack source.

### Explicitly not promised yet
- Full OptiFine shader-pack compatibility.
- Every custom uniform, directive, extension, or vendor-specific GLSL feature.
- A working shader renderer in the current alpha.
- Any FPS improvement from shader support.

## Low-end device policy

- Shader engine defaults to disabled until the pipeline passes runtime tests.
- Offer a global off switch and a vanilla fallback.
- Start with conservative framebuffer scale and shadow-map resolution.
- Enforce a configurable GPU-resource budget and release unused targets.
- Skip optional passes when allocation fails; never leave the game in a partially active shader state.
- Expose quality presets only after each preset has measured resource costs.

## Validation gates

A milestone is complete only when:
1. A Forge 47.2.0 client launches with the feature enabled and disabled.
2. A minimal test pack compiles and renders correctly.
3. Pack switching, resource reload, window resize, world exit, and graphics-context recreation release and rebuild resources safely.
4. Invalid packs fail back to vanilla without crashing the client.
5. At least one real OptiFine-format pack is tested and the supported/unsupported features are documented.
6. Frame time and memory use are measured against vanilla Forge on repeatable scenes.

## Implementation sequence

1. Add a client-only shader-pack catalog and safe archive validation.
2. Add parser and compatibility diagnostics with unit tests.
3. Establish a render-thread-owned program/resource lifecycle.
4. Implement a minimal pass graph and a test shader.
5. Add OptiFine-format compatibility incrementally, with a test pack for each supported feature.
6. Add settings UI and quality profiles after the pipeline is stable.

This document is an engineering plan. It does not mean shader loading, shader compilation, or shader rendering is implemented in the current build.