# Repository Import Status

## Added to this development branch

- ForgeGradle project configuration for Minecraft 1.20.1 / Forge 47.2.0 / Java 17.
- Mod entry point and client configuration.
- Client key mappings, settings and compatibility screens.
- Read-only chunk dispatcher diagnostics HUD.
- Mod presence scanner.
- Forge mod metadata and English UI strings.
- GitHub Actions workflow to build the Forge mod and upload the resulting JAR artifact.

## Still to import from the Alpha 09 source archive

The standalone cache implementation and its tests have not yet been committed as source files in this branch:

- `src/main/java/dev/opticore/cache/SmartChunkCache.java`
- `src/main/java/dev/opticore/cache/CacheEntry.java`
- `src/main/java/dev/opticore/cache/RenderCacheKey.java`
- `src/test/java/dev/opticore/cache/SmartChunkCacheSelfTest.java`
- `scripts/run-cache-tests.sh`

The Alpha 09 ZIP remains the complete local source snapshot. Do not treat the GitHub branch as a complete mirror of that archive yet.

## CI status

The workflow has been added, but no completed GitHub Actions run is visible yet. A successful JAR build has therefore not been confirmed. The next verification step is to confirm that Actions is enabled for the repository, then inspect the build job and fix any Forge compilation errors it reports.
