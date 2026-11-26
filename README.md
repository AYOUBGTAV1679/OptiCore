# OptiCore

An experimental, independent performance-modification project for Minecraft Java Edition 1.20.1 on Forge 47.2.0. The project is being developed without Embeddium or Oculus as dependencies.

> **Status: early alpha.** The current build provides configuration, an FPS HUD, hold-to-zoom, compatibility diagnostics, and chunk-renderer queue diagnostics. The bounded render-artifact cache and its invalidation lifecycle are infrastructure only: compiled chunk meshes are not cached or reused yet. No FPS improvement or OptiFine feature parity is claimed.

## Current features

- Forge 1.20.1 / Forge 47.2.0 / Java 17 target.
- Press **O** to open OptiCore settings.
- Hold **C** to zoom (configurable zoom factor).
- Optional FPS counter.
- Optional read-only chunk renderer queue diagnostics and bounded CPU mesh-buffer observation (the buffer-copy work is disabled unless diagnostics are enabled).
- Optional rolling frame-time monitor (current frame time and estimated 1% low FPS).
- Configurable bounded cache budget (8–512 MiB; default 32 MiB).
- Client lifecycle invalidation for chunk unloads, block edits, chunk-border neighbors, dimension unloads, and resource reloads.
- English and Arabic interface translations.
- Shader-pack catalog for ZIP and folder packs, bounded archive validation, metadata and option discovery, and per-pack option persistence.
- Render-thread GLSL compile/link diagnostics with a bounded shader-program cache.
- An opt-in fullscreen world composite that captures vanilla scene color/depth, executes through the validated render graph, and can run a selected shader-pack program only when it matches OptiCore's strict fullscreen interface contract.

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

OptiCore is being developed around one shader compiler, one validated render-pass graph, and one GPU resource manager.

The current alpha includes a limited, opt-in fullscreen world-composite path. It captures the completed vanilla scene's color and depth, uses live biome sky color blended with rain and thunder for atmospheric fog, runs a graph-scheduled composite pass, and restores the original scene if the pass fails. A selected shader pack can supply the composite program only when a paired vertex/fragment program meets OptiCore's narrow fullscreen contract, compiles and links on the current OpenGL driver, and exposes the required uniforms with the expected linked GL types. Recognized per-pack option overrides are applied before compilation.

This is **not general OptiFine shader-pack compatibility**. Most existing packs use terrain/entity programs, multiple render targets, extra uniforms, shadow passes, or other interfaces that this fullscreen path does not implement. The pack catalog and preflight report describe source readiness; only the specific fullscreen path is eligible for execution. The world pass is disabled by default and must be enabled explicitly with the **Experimental World Pass** toggle in OptiCore settings. A minimal pack implementing this exact interface is included at `examples/opticore-fullscreen-demo`; copy that folder into the instance's `shaderpacks` directory to test it. See [the fullscreen shader-pack integration notes](docs/ALPHA-12-FULLSCREEN-SHADER-PACKS.md) and [Unified Shader Engine Architecture](docs/UNIFIED-SHADER-ENGINE.md).

Dynamic terrain/entity shadows, shadow filtering, water reflections, full material/lightmap handling, complete OptiFine uniform parity, and general multi-pass pack execution remain unfinished. The generic chunk cache also remains storage/lifecycle infrastructure rather than a working compiled-mesh reuse optimization. No FPS improvement or broad shader compatibility is claimed.

## Engineering status

The current cache is a bounded, thread-safe LRU storage component with lifecycle invalidation, but it is not connected to mesh reuse and does not currently reduce chunk rebuild work. Client mixins observe vanilla chunk-layer rendering and completed section compilation; when Chunk Diagnostics is enabled, the rebuild observer makes bounded CPU copies for counters and discards them without retaining vanilla buffers. The branch passes the three configured Forge 1.20.1 CI build variants and packaged Mixin checks, but a live client launch is still required to confirm runtime injection. See [Mixin Renderer Integration](docs/MIXIN-RENDERER-INTEGRATION.md), [Renderer Cache Lifecycle](docs/RENDERER-CACHE-LIFECYCLE.md), and [Renderer Integration Research](docs/RENDERER-INTEGRATION-RESEARCH.md).

Performance must be evaluated against a repeatable unmodified-Forge baseline. The project's [performance validation plan](docs/PERFORMANCE-VALIDATION-PLAN.md) defines the required scenes, measurements, and pass/fail rules. No controlled benchmark has been completed yet.

## Planned work

1. Validate the fullscreen pass in a live Minecraft client across supported graphics modes and representative GPU drivers.
2. Expand the shader interface from the current fullscreen-only contract toward terrain, entity, material, and multi-pass programs.
3. Implement and integrate real shadow-map caster rendering and shadow sampling.
4. Add water/reflection and lighting paths through the same render graph, with explicit resource and fallback handling.
5. Connect immutable mesh snapshots to safe cache lookup, invalidation, upload, and reuse at the vanilla chunk compilation boundary.
6. Run controlled performance and compatibility benchmarks against an unmodified Forge baseline before claiming improvements.

## Build from source

Requirements: JDK 17 and internet access for Gradle dependencies.

```bash
./gradlew build
```

The Forge mod JAR is produced under `build/libs/`. The standalone cache regression suite can be run with:

```bash
bash scripts/run-cache-tests.sh
```

The static client-Mixin registration checks can be run with:

```bash
python3 scripts/check-mixin-config.py
python3 scripts/test-packaged-mixin.py
python3 scripts/check-packaged-mixin.py  # after gradle build
```

## License

No license has been selected yet. Until a license is added, all rights are reserved by default; do not assume the source is open for redistribution or reuse.
