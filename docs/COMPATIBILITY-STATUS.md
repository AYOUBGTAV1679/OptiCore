# Compatibility Status

Target environment: Minecraft Java Edition 1.20.1, Forge 47.2.0, Java 17.

## Explicit project constraints

- Embeddium: intentionally not a dependency.
- Oculus: intentionally not a dependency.
- OptiCore currently does not provide an alternative shader pipeline.

## User-requested mod list

The following names are a **test queue**, not a compatibility guarantee. No entry should be marked compatible until the exact mod build is installed and exercised in-game with OptiCore.

| Mod / family | Current status |
|---|---|
| AmbientSounds | Not tested |
| AppleSkin | Not tested |
| Architectury | Not tested |
| Backpacked | Not tested |
| BetterAdvancements | Not tested |
| BetterCombat | Not tested |
| BetterFog | Not tested |
| BetterFpsDist | Not tested |
| Bobby | Not tested |
| Born in Chaos | Not tested |
| Camera | Not tested |
| Cave Dweller | Not tested |
| Chat Heads | Not tested |
| Cloth Config | Not tested |
| CorgiLib | Not tested |
| CoroUtil | Not tested |
| Corpse | Not tested |
| CreativeCore | Not tested |
| Dynamic FPS | Not tested |
| Entity Model Features | Not tested |
| Entity Texture Features | Not tested |
| Essential | Not tested |
| FPS Reducer | Not tested |
| Framework | Not tested |
| From The Fog | Not tested |
| GeckoLib | Not tested |
| Geophilic | Not tested |
| JEI | Not tested |
| Oh The Trees You'll Grow | Not tested |
| Player Animation Lib | Not tested |
| Sound Physics Remastered | Not tested |
| TerraBlender | Not tested |
| Simple Voice Chat | Not tested |
| VoidFog | Not tested |
| What Are They Up To | Not tested |
| Weather2 | Not tested |
| Xaero Minimap | Not tested |
| YUNG's API | Not tested |
| YUNG's Cave Biomes | Not tested |

## What counts as a pass

A mod is only marked compatible after:
1. The game starts with the exact Minecraft, Forge, Java, and mod versions recorded.
2. A world loads and the relevant mod feature is exercised.
3. No OptiCore-related crash, rendering corruption, missing texture, or persistent log error occurs.
4. The test is repeated after a resource reload and world re-entry when relevant.

Mod-ID detection is informational only; it is not an interoperability test.
