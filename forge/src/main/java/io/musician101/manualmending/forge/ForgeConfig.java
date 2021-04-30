package io.musician101.manualmending.forge;

import io.musician101.manualmending.common.Config;
import java.io.IOException;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

public class ForgeConfig implements Config {

    private final ForgeConfigSpec.BooleanValue repairAll;
    private final ForgeConfigSpec spec;

    public ForgeConfig() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        repairAll = builder.define("repairAll", false);
        this.spec = builder.pop().build();
    }

    @Override
    public boolean repairAll() {
        return repairAll.get();
    }

    @Override
    public void load() throws IOException {
        ModLoadingContext.get().registerConfig(Type.SERVER, spec);
        spec.save();
    }
}
