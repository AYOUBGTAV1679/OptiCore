# OptiCore Alpha 10 — Frame-Time Monitor

## Added

The optional Frame Time Monitor adds a HUD line with:
- Latest measured frame time in milliseconds.
- A rolling 1% low FPS estimate derived from the 99th-percentile frame time over the most recent 120 rendered frames.

The monitor is disabled by default and can be enabled from the OptiCore settings screen. Its label is translated in English and Arabic. Sampling uses fixed-size buffers without per-frame allocations; the percentile is refreshed every 15 samples rather than sorting on every rendered frame. HUD text formatting is throttled to at most four updates per second, and the render loop reuses the cached text between updates.

A plain-JDK regression test checks sampling, finite percentile output, reset behavior, and the cached HUD text as part of the CI test script.

## Interpretation

Frame time is the time between consecutive HUD render samples, not a GPU timer query. The 1% low is a short rolling estimate and is not a substitute for a controlled benchmark. It can be affected by scene changes, loading, background activity, and the first seconds after enabling it.

## Validation status

This feature has been added to the Forge source branch. The earlier Alpha 11 CI run completed successfully for its then-current commit, including the Forge build and artifact upload. The subsequent HUD-allocation refinement is a newer commit and still needs its own CI run and in-game launch. No performance improvement is claimed; this is a measurement tool intended to make later renderer work easier to evaluate.
