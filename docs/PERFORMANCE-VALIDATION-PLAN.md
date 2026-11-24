# Performance Validation Plan

## Purpose

This document defines the minimum repeatable test procedure required before OptiCore can claim a performance improvement. Queue counters and cache unit tests are diagnostics, not performance evidence.

## Fixed test environment

Record these values for every run:

- Minecraft: 1.20.1
- Forge: 47.2.0
- Java runtime and vendor
- Operating system / Android version
- Launcher (official launcher, PojavLauncher, or other)
- Device model, CPU, GPU, RAM, and available memory
- Resolution, render distance, simulation distance, graphics preset, VSync, FPS cap
- Exact OptiCore build SHA and exact mod list with versions

Do not compare runs with different settings or mod lists.

## Test scenes

Use the same world seed and coordinates for each run. Keep a copy of the test world.

1. **Static scene:** stand still for 120 seconds after a 60-second warm-up.
2. **Traversal scene:** follow a fixed route through previously unexplored terrain for 180 seconds.
3. **Revisit scene:** return along the same route to measure behavior when chunks are revisited.
4. **Edit/invalidation scene:** place and break blocks near chunk borders, then observe rebuild and upload activity.
5. **Reload scene:** reload resources and re-enter the world; check for stale geometry, missing textures, crashes, or memory growth.

Run each scene at least three times for both the unmodified Forge baseline and OptiCore. Alternate baseline/OptiCore order where practical to reduce thermal and background-load bias.

## Measurements

Capture at minimum:

- Average FPS and 1% low FPS, if a reliable tool is available
- Frame-time distribution (median, 95th percentile, 99th percentile)
- Chunk build queue, pending upload count, and free builder buffers
- Memory use before and after each scene
- Startup time and world-load time
- Crashes, rendering artifacts, and relevant log errors

Report the median across the three runs and preserve raw logs. Do not infer FPS gains from queue counts alone.

## Pass/fail rules

A candidate optimization passes only when:

- The same scene and settings were used for baseline and candidate.
- No stale chunk geometry or lighting appears after edits, border changes, resource reloads, or world transitions.
- No resource leak or sustained memory growth is observed.
- No OptiCore-related crash or persistent error occurs.
- The measured improvement exceeds run-to-run noise and does not cause a material regression in 1% lows or frame-time spikes.

If results are neutral or mixed, report them as such. If no reliable frame-time capture is available, mark performance as **not measured**.

## Current status

No controlled baseline-versus-OptiCore benchmark has been completed. The existing HUD is read-only instrumentation, and the current generic cache has no producer connected to Minecraft's compiled chunk meshes. Therefore, no FPS improvement is currently established.

## Required evidence for a future release

Attach the exact build identifier, device/settings sheet, raw measurements, logs, and a concise comparison table to the release notes. Compatibility with third-party mods must be validated separately using the procedure in [COMPATIBILITY-STATUS.md](COMPATIBILITY-STATUS.md).
