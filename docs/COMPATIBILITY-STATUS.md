# Compatibility Status

Target environment: Minecraft Java Edition 1.20.1, Forge 47.2.0, Java 17.

## Renderer and shader support

OptiCore has an experimental, opt-in fullscreen scene-composite path. It captures the vanilla scene color/depth and can apply OptiCore's built-in color grading and atmospheric fog, or run a shader pack that satisfies OptiCore's limited fullscreen shader contract.

This is **not** a replacement for the complete Minecraft shader pipeline. In particular:

- General OptiFine shader-pack compatibility is not implemented.
- Oculus shader-pack compatibility is not implemented; Oculus is intentionally not a dependency.
- Embeddium is intentionally not a dependency.
- Shader packs that require deferred rendering, multiple world render passes, shadow-map generation, entity/block-entity passes, custom material buffers, or other unsupported OptiFine/Oculus interfaces are not supported by this experimental path. If a selected pack compiles but fails during its draw, OptiCore retries that frame with its built-in composite; if that also fails, the original captured scene is restored where possible.
- The experimental world pass is disabled by default and must be explicitly enabled in the client configuration.

The included fullscreen demo pack documents the supported contract. A successful preflight or shader compile only confirms that the pack satisfies the limited interface; it does not establish compatibility with arbitrary shader packs or prove correct in-game visual output.

## Implemented foundations — not complete rendering features

| Area | Current evidence | Status |
|---|---|---|
| Fullscreen scene composite | Opt-in capture/composite pass with built-in fallback and limited pack contract | Experimental |
| World color grading | Exposure, contrast, and saturation controls applied by fullscreen composite | Implemented in experimental pass |
| Atmospheric fog | Configurable range/strength and weather-aware sky-derived color | Implemented in experimental pass |
| Shader-pack options | Bounded discovery of boolean, enum, and numeric declarations; per-pack persistence; validated GLSL macro/constant overrides before compile | Partial; contract-dependent |
| Render graph | Validated pass/resource model and executor used by the experimental fullscreen path | Foundation |
| Mesh cache | Bounded generic CPU-artifact cache and lifecycle invalidation; no live mesh cache read/write path | Foundation only |
| Dynamic shadows | Camera/depth-target primitives; no verified terrain/entity caster rendering or shadow sampling | Not implemented |
| Water reflections | No verified water mask, normals, or reflection pass integrated into world rendering | Not implemented |
| General shader compatibility | No complete OptiFine/Oculus-compatible pipeline | Not implemented |

Do not interpret the presence of a class, configuration option, diagnostic counter, or passing unit test as proof that a complete in-game rendering feature is operational.

## User-requested mod list

The following names are a **test queue**, not a compatibility guarantee. No entry should be marked compatible until the exact mod build is installed and exercised in-game with OptiCore.

| Mod / family | Current status |
|---|---|
| AmbientSounds | Not tested |
| AppleSkin | Not tested |
| Architectury | Not tested |
| Backpacked | Not tested |
| BetterAdvancements | Not tested |
| BetterCombat | Not tested |
| BetterFog | Not tested |
| BetterFpsDist | Not tested |
| Bobby | Not tested |
| Born in Chaos | Not tested |
| Camera | Not tested |
| Cave Dweller | Not tested |
| Chat Heads | Not tested |
| Cloth Config | Not tested |
| CorgiLib | Not tested |
| CoroUtil | Not tested |
| Corpse | Not tested |
| CreativeCore | Not tested |
| Dynamic FPS | Not tested |
| Entity Model Features | Not tested |
| Entity Texture Features | Not tested |
| Essential | Not tested |
| FPS Reducer | Not tested |
| Framework | Not tested |
| From The Fog | Not tested |
| GeckoLib | Not tested |
| Geophilic | Not tested |
| JEI | Not tested |
| Oh The Trees You'll Grow | Not tested |
| Player Animation Lib | Not tested |
| Sound Physics Remastered | Not tested |
| TerraBlender | Not tested |
| Simple Voice Chat | Not tested |
| VoidFog | Not tested |
| What Are They Up To | Not tested |
| Weather2 | Not tested |
| Xaero Minimap | Not tested |
| YUNG's API | Not tested |
| YUNG's Cave Biomes | Not tested |

## What counts as a pass

A mod is only marked compatible after:
1. The game starts with the exact Minecraft, Forge, Java, and mod versions recorded.
2. A world loads and the relevant mod feature is exercised.
3. No OptiCore-related crash, rendering corruption, missing texture, or persistent log error occurs.
4. The test is repeated after a resource reload and world re-entry when relevant.

Mod-ID detection is informational only; it is not an interoperability test.

## Validation boundary

CI builds and standalone regression tests validate compilation and isolated logic. They do not prove that Mixins apply correctly in a live Minecraft client, that the experimental composite looks correct across GPUs, or that performance improves. Those claims require a live-client test on the stated target environment and recorded logs/results.
