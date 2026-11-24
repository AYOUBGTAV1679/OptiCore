package dev.opticore.compat;

import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Reports installed-mod presence and explicit project policy.
 * Detection is not a certification of runtime compatibility.
 */
public final class ModCompatibilityScanner {
    public record Entry(String displayName, String modId, boolean present,
                        boolean excluded, String note) {}

    private static final String[][] TARGETS = {
        {"AmbientSounds", "ambientsounds", "Audio integration; verify no client-side regressions"},
        {"AppleSkin", "appleskin", "HUD overlay integration test required"},
        {"Architectury API", "architectury", "Shared API dependency; verify Forge build"},
        {"Backpacked", "backpacked", "Container and item rendering test required"},
        {"BetterAdvancements", "betteradvancements", "GUI screen integration test required"},
        {"Better Combat", "bettercombat", "Player animation integration test required"},
        {"BetterFog", "betterfog", "Fog pipeline integration test required"},
        {"BetterFpsDist", "betterfpsdist", "Chunk-distance behavior test required"},
        {"Bobby", "bobby", "Verify Forge build and distant-chunk behavior"},
        {"Born in Chaos", "born_in_chaos", "Entity and block rendering test required"},
        {"Camera", "camera", "Camera/render-stage integration test required"},
        {"Cave Dweller", "cave_dweller", "Entity rendering test required"},
        {"Chat Heads", "chat_heads", "HUD overlay integration test required"},
        {"Cloth Config", "cloth_config", "Configuration UI dependency; verify Forge build"},
        {"CorgiLib", "corgilib", "Library dependency; verify target version"},
        {"CoroUtil", "coroutil", "Library dependency; verify target version"},
        {"Corpse", "corpse", "Block entity rendering test required"},
        {"CreativeCore", "creativecore", "Library and GUI integration test required"},
        {"Dynamic FPS", "dynamic_fps", "Window focus and background throttling test required"},
        {"Entity Model Features", "entity_model_features", "Custom model integration test required"},
        {"Entity Texture Features", "entity_texture_features", "Texture integration test required"},
        {"Essential", "essential", "Overlay and renderer integration test required"},
        {"Framework", "framework", "Library dependency; verify target version"},
        {"From The Fog", "fromtheshadows", "Entity rendering test required; verify mod ID"},
        {"GeckoLib", "geckolib", "Animated entity rendering test required"},
        {"Geophilic", "geophilic", "World-generation and block rendering test required"},
        {"JEI", "jei", "GUI overlay integration test required"},
        {"Oh The Trees You'll Grow", "ohthetreesyoullgrow", "Foliage rendering test required"},
        {"Player Animation Library", "playeranimator", "Player animation integration test required"},
        {"Sound Physics Remastered", "sound_physics_remastered", "Audio mod; renderer should not interfere"},
        {"TerraBlender", "terrablender", "World-generation library; verify target version"},
        {"Simple Voice Chat", "voicechat", "Audio and overlay integration test required"},
        {"VoidFog", "voidfog", "Fog pipeline integration test required"},
        {"What Are They Up To", "whataretheyup to", "Entity rendering test required; verify mod ID"},
        {"Weather2", "weather2", "Weather and particle rendering test required"},
        {"Xaero's Minimap", "xaerominimap", "HUD overlay integration test required"},
        {"YUNG's API", "yungsapi", "Library dependency; verify Forge build"},
        {"YUNG's Better Cave Biomes", "bettercaves", "World-generation and block rendering test required"},
        {"Embeddium", "embeddium", "Explicitly excluded by the OptiCore project", "excluded"},
        {"Oculus", "oculus", "Explicitly excluded by the OptiCore project", "excluded"}
    };

    private ModCompatibilityScanner() {}

    public static List<Entry> scan() {
        List<Entry> result = new ArrayList<>(TARGETS.length);
        ModList mods = ModList.get();
        for (String[] target : TARGETS) {
            boolean excluded = target.length > 3 && "excluded".equals(target[3]);
            result.add(new Entry(target[0], target[1], mods.isLoaded(target[1]),
                    excluded, target[2]));
        }
        return List.copyOf(result);
    }
}
