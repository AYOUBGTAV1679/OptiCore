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

1. **Pack discovery** — implemented as an initial catalog for top-level shader-pack folders and ZIP files under `shaderpacks/`. Selection is persisted. This stage does not extract archives. ZIP discovery rejects unsafe entry paths and archives with more than 20,000 entries; the catalog does not extract archive contents.
2. **Pack metadata and options** — initial read-only inspection is implemented for `shaders/shaders.properties`, including property count plus `screen` and `sliders` identifiers. A preview screen lists declared identifiers. These are not editable controls and do not change shader behavior yet. A bounded source inventory counts `.vsh`, `.fsh`, and `.glsl` files. A separate preflight pairs vertex and fragment files by relative path and reports complete, vertex-only, and fragment-only program sets for folders and ZIP archives. This is structural pairing only; it does not parse GLSL, validate interfaces, or compile programs.
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

1. Harden the client-only shader-pack catalog with bounded ZIP entry enumeration and path validation (20,000-entry cap), covered by standalone regression tests.
2. Expand the metadata parser, source inventory, and compatibility diagnostics with unit tests.
3. Establish a render-thread-owned program/resource lifecycle.
4. Implement a minimal pass graph and a test shader.
5. Add OptiFine-format compatibility incrementally, with a test pack for each supported feature.
6. Add editable settings UI and quality profiles after the pipeline is stable.

This document is an engineering plan. It does not mean shader loading, shader compilation, or shader rendering is implemented in the current build.

## Alpha update — option persistence groundwork

A small bounded storage component now persists raw option override strings independently for each pack and option identifier. It rejects unsafe pack names, malformed identifiers, oversized values, and oversized configuration files; writes use a temporary file and atomic replacement where supported. The standalone test suite covers pack isolation, save/load/removal, and input bounds.

This is storage infrastructure only. The current options screen remains read-only: it does not create widgets from declarations, write values through this store, interpret option types, or apply values to GLSL uniforms. Those steps require a verified parser for the shader-pack property semantics before UI controls are introduced.


## Alpha update — shader program pairing preflight

The pack manager now reports how many program paths have both a vertex and fragment stage, plus unmatched stages. Pairing uses the full relative path under `shaders/`, avoiding false matches when different subfolders reuse the same filename. The scan is bounded by archive-entry and program-count limits, rejects unsafe ZIP paths, and is covered by folder and ZIP regression tests. These counts are a structural preflight only; they do not establish GLSL validity or runtime compatibility.


## Alpha update — bounded GLSL source preflight

A source-level preflight now reads vertex and fragment source files from shader-pack folders and ZIP archives under strict per-file, aggregate-byte, file-count, and archive-entry limits. It reports stages without a recognizable `void main(...)` entry point, detected `#version` declarations, and simple `#include` directive counts. Unsafe ZIP paths are ignored.

This scan is intentionally heuristic: it does not strip comments, resolve includes, parse GLSL grammar, validate stage interfaces, invoke a GPU compiler, or prove that a program will link. A detected `main` or version is only a diagnostic signal. Shader compilation and rendering remain unimplemented.


## Alpha update — lexical preflight and bounded include expansion

The GLSL preflight now masks line/block comments and quoted text before checking for a recognizable `void main(...)` entry point. It extracts basic `#version` and `#extension` directives and counts include directives. Directive matching is constrained to a single line to avoid whitespace-regex matches spanning unrelated lines.

A bounded include resolver now expands quoted or angle-bracket includes from shader-pack folders and ZIP archives without extracting ZIP contents. It rejects absolute paths, traversal, unsafe archive names, and symbolic links; detects include cycles; and enforces limits of 16 nested levels, 256 distinct files, 512 KiB per source, and 4 MiB total source bytes. Folder, ZIP, nested include, cycle, and traversal cases have standalone regression coverage.

These components are still preflight/compatibility infrastructure. They do not parse full GLSL grammar, process conditional preprocessing, compile or link GPU programs, or render shader passes. The resolver is not yet wired into a runtime shader compiler.
