package dev.opticore.compat;

import net.minecraftforge.fml.ModList;
import java.util.ArrayList;
import java.util.List;

/** Presence scanner only; detection does not certify runtime compatibility. */
public final class ModCompatibilityScanner {
    public record Entry(String displayName, String modId, boolean present, String note) {}
    private static final String[][] TARGETS = {
        {"GeckoLib", "geckolib", "Entity animation integration test required"},
        {"Entity Model Features", "entity_model_features", "Custom model integration test required"},
        {"Entity Texture Features", "entity_texture_features", "Texture integration test required"},
        {"Better Combat", "bettercombat", "Player animation integration test required"},
        {"Player Animator", "playeranimator", "Animation integration test required"},
        {"Weather2", "weather2", "Weather rendering integration test required"},
        {"AmbientSounds", "ambientsounds", "Audio mod; renderer should not interfere"},
        {"Sound Physics Remastered", "sound_physics_remastered", "Audio mod; renderer should not interfere"},
        {"JEI", "jei", "GUI overlay integration test required"},
        {"Xaero's Minimap", "xaerominimap", "HUD overlay integration test required"},
        {"Bobby", "bobby", "Confirm correct Forge build and chunk-distance behavior"},
        {"BetterFpsDist", "betterfpsdist", "Chunk-distance integration test required"},
        {"Born in Chaos", "born_in_chaos", "Entity rendering integration test required"},
        {"Cave Dweller", "cave_dweller", "Entity rendering integration test required"},
        {"BetterFog", "betterfog", "Fog pipeline integration test required"},
        {"VoidFog", "voidfog", "Fog pipeline integration test required"}
    };
    private ModCompatibilityScanner() {}
    public static List<Entry> scan() {
        List<Entry> result = new ArrayList<>();
        ModList mods = ModList.get();
        for (String[] target : TARGETS)
            result.add(new Entry(target[0], target[1], mods.isLoaded(target[1]), target[2]));
        return result;
    }
}
