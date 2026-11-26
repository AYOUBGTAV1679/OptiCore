# Alpha 13 — weather-responsive atmospheric fog

## Implemented

The opt-in fullscreen world composite now uses a validated, configurable depth-fog interval:

- **Fog strength** is controlled by the existing World Fog Strength setting.
- **Fog start** and **Fog end** distances can be adjusted in OptiCore Settings.
- The fragment shader reconstructs view-space distance from the captured scene depth and inverse projection matrix.
- Fog color is derived from the camera biome's live sky color.
- Rain and thunderstorms blend the sky tint toward an overcast tone.
- Increasing rain/thunder also strengthens the haze and moves its distance interval closer to the camera.
- Invalid or inverted persisted fog ranges are clamped to a safe interval before uniforms are uploaded.

The selected fullscreen shader-pack contract requires active, correctly typed uniforms for scene color/depth, warmth, fog strength/color/start/end, exposure, contrast, saturation, and inverse projection. The included demo pack uses the same interface and keeps its editable fog sliders effective alongside OptiCore's runtime fog settings.

## Settings

Open **O → OptiCore Settings**:

- **Experimental World Pass** must be enabled explicitly.
- **World Fog Strength** controls the base haze amount.
- **Fog Start Distance** sets where haze begins.
- **Fog End Distance** sets where haze reaches full strength.
- **World Color Warmth** controls the built-in color grade.

The pass remains disabled by default. If a selected shader pack satisfies the narrow fullscreen contract, its program is used; otherwise OptiCore uses its built-in composite and records the pack diagnostic.

## Regression coverage

The standalone Java 17 regression suite checks:

- fallback behavior for non-finite fog settings;
- ordering and bounds of configured fog distances;
- weather-dependent fog strength and range;
- sky-color blending for clear, rainy, and storm conditions;
- required fullscreen fog uniforms and their GLSL types;
- demo-pack folder and ZIP discovery, option metadata, and source overrides.

The latest verified CI build matrix covers Forge 1.20.1 versions 47.0.0, 47.2.0, and 47.4.29. This validates source compilation and automated tests only. It does not replace a live Minecraft test on the target GPU.

## Remaining limits

This is a depth-based fullscreen haze effect, not Minecraft's full native fog pipeline and not complete OptiFine fog compatibility. It does not yet implement underwater/eye-medium transitions, height fog, volumetric fog, per-material fog rules, or terrain/entity shader stages. The entire fullscreen world pass remains experimental and opt-in.
