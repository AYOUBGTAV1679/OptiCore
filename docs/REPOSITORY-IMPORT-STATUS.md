# Repository Import Status

## Present on this development branch

- ForgeGradle project configuration for Minecraft 1.20.1 / Forge 47.2.0 / Java 17.
- Mod entry point and client configuration.
- Client key mappings, settings and compatibility screens.
- Read-only chunk dispatcher diagnostics HUD.
- Mod presence scanner.
- Forge mod metadata and English UI strings.
- Bounded, thread-safe, Minecraft-independent LRU cache implementation:
  - `SmartChunkCache`
  - `CacheEntry`
  - `RenderCacheKey`
- Dependency-free cache regression and concurrency self-tests.
- `scripts/run-cache-tests.sh` test runner.
- GitHub Actions workflow runs cache tests, then builds the Forge mod and uploads the resulting JAR artifact.

## Scope and limitations

The cache is a standalone data structure only. It is not connected to Minecraft's chunk mesh compilation, GPU buffers, or the renderer. The diagnostics HUD reads dispatcher counters; it does not itself improve FPS or change chunk scheduling.

The branch now contains the previously missing cache implementation and test files from the Alpha 09 source snapshot. A successful Forge build and in-game test are still required before calling this a working mod release.

## CI verification

The workflow runs on pushes to `main` and `development/**`, pull requests targeting `main`, and manual dispatch. It first runs the standalone Java cache regression tests, then runs the ForgeGradle build and uploads the JAR if successful. Check the Actions run result before distributing a build.
