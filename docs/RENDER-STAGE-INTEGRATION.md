# Forge 47.2.0 Render-Level Stage Integration

## Verified event contract

Source inspected from the Forge 1.20.x RenderLevelStageEvent implementation:

- The event is fired on the Forge main event bus on the logical client.
- It exposes the current stage, LevelRenderer, pose matrix, projection matrix, render tick, partial tick, camera, and frustum.
- AFTER_SKY is called after sky rendering (including when the sky itself is hidden).
- AFTER_SOLID_BLOCKS, AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS, and AFTER_CUTOUT_BLOCKS are associated with their corresponding chunk RenderType layers.
- AFTER_ENTITIES, AFTER_BLOCK_ENTITIES, AFTER_TRANSLUCENT_BLOCKS, AFTER_TRIPWIRE_BLOCKS, AFTER_PARTICLES, and AFTER_WEATHER provide later world-render insertion points.
- AFTER_LEVEL fires after LevelRenderer.renderLevel finishes.
- RegisterStageEvent can register a custom stage. If a RenderType is supplied, Forge fires it automatically after that layer; if null, the stage must be invoked manually by the code that owns the render flow.

## Consequences for OptiCore

This is a better-supported extension point for an OptiCore-owned world pass than assuming that the late AFTER_LEVEL event is the only available stage. It still does not provide a scene-color copy, a depth texture, or a shader-pack-compatible fullscreen pipeline automatically.

A safe first rendering prototype should:

1. Register a uniquely named OptiCore stage on the mod event bus.
2. Choose an explicit insertion point and document its ordering relative to vanilla layers.
3. Render only OptiCore-owned geometry/resources in the first pass; do not sample the active main color attachment while simultaneously drawing back into it.
4. Capture and restore every OpenGL state changed by the pass, including framebuffer, viewport, program, texture bindings, blend, depth, cull, and write masks.
5. Gate the pass behind a disabled-by-default client option and skip cleanly when no world, target, or valid shader program exists.
6. Validate in a real Forge 47.2.0 client before claiming that any shader pack changes the Minecraft world.

## Current implementation boundary

OptiCore currently observes AFTER_LEVEL for read-only scene snapshots. It does not yet register a custom render stage or issue world-render draw calls. The event contract above is verified API research, not a completed renderer feature.
