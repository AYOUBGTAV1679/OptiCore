# Alpha 14 — fullscreen world color grading

## Implemented

The opt-in fullscreen world composite now applies three configurable color controls before its atmospheric fog blend:

- **Exposure** multiplies scene RGB values. Supported range: 0.5–1.5.
- **Contrast** scales color distance around middle gray (0.5). Supported range: 0.5–1.5.
- **Saturation** blends between luminance-only output and the graded color. Supported range: 0–2.

These controls are available from **O → World Color Grading** and are persisted through the existing Forge configuration. They affect only OptiCore's experimental fullscreen world composite; they do not alter Minecraft's native lighting engine, lightmaps, block light, or entity material shaders.

The built-in composite and the narrow fullscreen shader-pack contract both receive the same three frame uniforms:

- `float uExposure`
- `float uContrast`
- `float uSaturation`

The linked shader program must expose these uniforms with active float types. A selected pack that does not implement the updated contract is rejected for the fullscreen path and the built-in composite is used instead.

## Included demo pack

The included demo pack has independent per-pack options for warmth, exposure scale, contrast scale, saturation scale, and fog distances. These are GLSL constants substituted before compile/link. The global OptiCore color controls are applied separately, so both sets of controls can be tested.

## Validation and limits

The standalone contract regression suite verifies that all required color-grading uniforms are present and correctly typed at source level, and that the demo's option overrides remain discoverable and are applied to GLSL source. The Forge CI matrix checks compilation and packaged Mixin registration.

This feature is a fullscreen color-grade pass, not physically based lighting, gamma calibration, tone mapping, HDR, or a replacement for Minecraft's lightmap pipeline. It remains experimental and disabled unless **Experimental World Pass** is enabled. Live GPU/client validation is still required.
