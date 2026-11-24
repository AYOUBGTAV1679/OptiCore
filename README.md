# OptiCore

An experimental, independent performance-modification project for Minecraft Java Edition 1.20.1 on Forge 47.2.0. The project is being developed without Embeddium or Oculus as dependencies.

> **Status: early alpha.** The current build provides configuration, an FPS HUD, hold-to-zoom, compatibility diagnostics, and chunk-renderer queue diagnostics. The bounded render-artifact cache and its invalidation lifecycle are infrastructure only: compiled chunk meshes are not cached or reused yet. No FPS improvement or OptiFine feature parity is claimed.

## Current features

- Forge 1.20.1 / Forge 47.2.0 / Java 17 target.
- Press **O** to open OptiCore settings.
- Hold **C** to zoom (configurable zoom factor).
- Optional FPS counter.
- Optional read-only chunk renderer queue diagnostics.
- Configurable bounded cache budget (8–512 MiB; default 32 MiB).
- Client lifecycle invalidation for chunk unloads, block edits, chunk-border neighbors, dimension unloads, and resource reloads.
- English and Arabic interface translations.

## Installation

1. Install Minecraft Java Edition 1.20.1 with Forge **47.2.0** and Java 17.
2. Download the latest successful **OptiCore-Forge-1.20.1** artifact from the repository's GitHub Actions workflow.
3. Extract the artifact ZIP and place the included OptiCore JAR in the instance's `mods` folder.
4. Launch the Forge 1.20.1 profile.

Do not put the artifact ZIP itself in the `mods` folder. Back up the instance before testing alpha builds.

## Compatibility

OptiCore is intended to work without Embeddium or Oculus. Compatibility with other rendering, animation, audio, world-generation, and utility mods is **not yet certified**. A mod being detected by the compatibility screen does not mean it has passed an in-game test. See [the compatibility status document](docs/COMPATIBILITY-STATUS.md).

## Engineering status

The current cache is a bounded, thread-safe LRU storage component with lifecycle invalidation. It does not yet connect to the vanilla chunk mesh compilation boundary, so it does not currently reduce chunk rebuild work. See [Renderer Cache Lifecycle](docs/RENDERER-CACHE-LIFECYCLE.md) and [Renderer Integration Research](docs/RENDERER-INTEGRATION-RESEARCH.md).

Performance must be evaluated against a repeatable unmodified-Forge baseline. The project's [performance validation plan](docs/PERFORMANCE-VALIDATION-PLAN.md) defines the required scenes, measurements, and pass/fail rules. No controlled benchmark has been completed yet.

## Planned work

1. Verify a safe hook at the chunk mesh compilation boundary.
2. Implement CPU-side immutable mesh artifact reuse with complete invalidation and regression tests.
3. Measure performance against an unmodified Forge baseline on repeatable scenes and low-end hardware.
4. Add optional rendering features only after the core renderer path is stable.
5. Validate compatibility with the user's target mod list through actual game launches and gameplay tests.

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
