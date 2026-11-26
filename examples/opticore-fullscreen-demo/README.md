# OptiCore Fullscreen Contract Demo

This is a minimal shader pack for testing OptiCore's current **fullscreen composite contract**. It is not a general OptiFine shader pack.

## Install

1. Open the Minecraft instance directory.
2. Open or create `shaderpacks`.
3. Copy the `opticore-fullscreen-demo` folder into `shaderpacks`.
4. Launch Minecraft 1.20.1 with OptiCore.
5. Open **O → Shaders**, select **opticore-fullscreen-demo**, and confirm that the screen reports a fullscreen world-pass program as ready.
6. Open **Options** on the shader pack to change the warmth scale and fog distances.
7. In OptiCore settings, enable **Experimental World Pass**.

The pass remains opt-in. Disable it if your graphics driver or rendering setup shows artifacts.

## Contract

The demo uses `gl_VertexID` to generate a fullscreen triangle and declares exactly the interface required by OptiCore:

- `vUv` vertex-to-fragment varying
- `fragColor` fragment output
- `uSceneColor` and `uSceneDepth` sampler2D inputs
- `uWarmth` and `uFogStrength` float controls
- `uFogColor` vec3 supplied from live biome sky color blended with rain/thunder
- `uInverseProjection` mat4 for depth reconstruction

OptiCore compiles and links the selected program on the render thread and verifies the linked uniform types before using it. The demo also exposes three editable numeric sliders: `WARMTH_SCALE`, `FOG_NEAR`, and `FOG_FAR`. Their persisted values are substituted into shader source before compile/link.

The demo does not test terrain/entity shading, shadows, reflections, or multi-pass OptiFine compatibility.
