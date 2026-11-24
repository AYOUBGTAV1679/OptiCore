# OptiCore Alpha 11 — Smooth Zoom

## Added

The hold-to-zoom feature now supports smooth transitions in and out instead of switching the field of view immediately. The smoothing option is enabled by default and can be disabled in the settings screen. The existing zoom factor remains configurable through the Forge config value.

The controller uses a time-based exponential interpolation, clamps large frame-time deltas, and resets safely when zoom is disabled or the player is unavailable. This keeps the transition duration reasonably consistent across different frame rates.

## Validation

A plain-JDK regression test covers immediate zoom, immediate release, and bounded smooth transition values. It is included in the CI test script. A full Forge build and in-game visual verification are still required.
