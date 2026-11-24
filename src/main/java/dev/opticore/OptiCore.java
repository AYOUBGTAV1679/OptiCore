package dev.opticore;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

@Mod(OptiCore.MOD_ID)
public class OptiCore {
    public static final String MOD_ID = "opticore";

    public OptiCore() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, OptiCoreConfig.SPEC);
    }
}
