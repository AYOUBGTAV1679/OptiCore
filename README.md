# OptiCore

An experimental, independent performance-modification project for Minecraft Java Edition 1.20.1 on Forge 47.2.0. The project is being developed without Embeddium or Oculus as dependencies.

> **Status: early alpha.** The current build provides configuration, an FPS HUD, hold-to-zoom, compatibility diagnostics, and chunk-renderer queue diagnostics. The bounded render-artifact cache and its invalidation lifecycle are infrastructure only: compiled chunk meshes are not cached or reused yet. No FPS improvement or OptiFine feature parity is claimed.

## Current features

- Forge 1.20.1 / Forge 47.2.0 / Java 17 target.
- Press **O** to open OptiCore settings.
- Hold **C** to zoom (configurable zoom factor).
- Optional FPS counter.
- Optional read-only chunk renderer queue diagnostics.
- Optional rolling frame-time monitor (current frame time and estimated 1% low FPS).
- Configurable bounded cache budget (8–512 MiB; default 32 MiB).
- Client lifecycle invalidation for chunk unloads, block edits, chunk-border neighbors, dimension unloads, and resource reloads.
- English and Arabic interface translations.
- Experimental shader-pack catalog: detects ZIP and folder packs, stores the selected pack, validates ZIP entry paths with a 20,000-entry cap, and lexically reads bounded `shaders/shaders.properties` screen/slider declarations. These are discovery and inspection tools only.

## Installation

1. Install Minecraft Java Edition 1.20.1 with Forge **47.2.0** and Java 17.
2. Download the latest successful **OptiCore-Forge-1.20.1** artifact from the repository's GitHub Actions workflow.
3. Extract the artifact ZIP and place the included OptiCore JAR in the instance's `mods` folder.
4. Launch the Forge 1.20.1 profile.

Do not put the artifact ZIP itself in the `mods` folder. Back up the instance before testing alpha builds.

## Compatibility

OptiCore is intended to work without Embeddium or Oculus. Compatibility with other rendering, animation, audio, world-generation, and utility mods is **not yet certified**. A mod being detected by the compatibility screen does not mean it has passed an in-game test. See [the compatibility status document](docs/COMPATIBILITY-STATUS.md).

The frame-time monitor is disabled by default and can be enabled in the settings screen. It is a diagnostic measurement, not an FPS-boost feature; see [Alpha 10 Frame-Time Monitor](docs/ALPHA-10-FRAME-TIME-MONITOR.md).

Smooth zoom transitions are enabled by default and can be switched off in settings. See [Alpha 11 Smooth Zoom](docs/ALPHA-11-SMOOTH-ZOOM.md).

## Unified shader direction

OptiCore is planned to use **one integrated shader engine**, following the OptiFine-style shader-pack workflow. Shadows, water, reflections, entity/terrain shading, and post-processing will run through the same program compiler, render-pass graph, and GPU resource manager—not separate shader systems. The compatibility target is the OptiFine shader-pack format for Minecraft 1.20.1, implemented incrementally with a vanilla fallback and low-memory safeguards.

**Shader packs are not loadable in the current alpha.** The catalog, bounded ZIP validation, and read-only metadata preview are implemented. A bounded per-pack option-override storage API and standalone regression tests for storage, archive validation, and property tokenization are present as groundwork, but the options screen is still read-only and does not yet edit or apply those values. No shader compiler, render-pass pipeline, or OptiFine pack rendering compatibility has been implemented or runtime-tested yet. See [Unified Shader Engine Architecture](docs/UNIFIED-SHADER-ENGINE.md) for the staged implementation and validation requirements.

## Engineering status

The current cache is a bounded, thread-safe LRU storage component with lifecycle invalidation. It does not yet connect to the vanilla chunk mesh compilation boundary, so it does not currently reduce chunk rebuild work. MixinGradle and a Mixin config are prepared for future client-side renderer hooks, but the config contains no active injections yet. See [Renderer Cache Lifecycle](docs/RENDERER-CACHE-LIFECYCLE.md), [Renderer Integration Research](docs/RENDERER-INTEGRATION-RESEARCH.md), and [Mixin Renderer Integration](docs/MIXIN-RENDERER-INTEGRATION.md).

Performance must be evaluated against a repeatable unmodified-Forge baseline. The project's [performance validation plan](docs/PERFORMANCE-VALIDATION-PLAN.md) defines the required scenes, measurements, and pass/fail rules. No controlled benchmark has been completed yet.

## Planned work

1. Add and runtime-verify a minimal client-only instrumentation hook at the chunk rebuild boundary.
2. Implement owned immutable CPU mesh snapshots and safe upload reconstruction, with complete invalidation and regression tests.
3. Measure performance against an unmodified Forge baseline on repeatable scenes and low-end hardware.
4. Extend the shader catalog with tested compatibility parsing, then implement program compilation, the render-pass graph, and OptiFine-format support.
5. Add optional visual features only through that shared pipeline after the core renderer path is stable.
6. Validate compatibility with the target mod list and shader packs through actual game launches and gameplay tests.

## Build from source

Requirements: JDK 17 and internet access for Gradle dependencies.

```bash
./gradlew build
```

The Forge mod JAR is produced under `build/libs/`. The standalone cache regression suite can be run with:

```bash
bash scripts/run-cache-tests.sh
```

## License

No license has been selected yet. Until a license is added, all rights are reserved by default; do not assume the source is open for redistribution or reuse.
