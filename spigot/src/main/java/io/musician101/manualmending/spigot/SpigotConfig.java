package io.musician101.manualmending.spigot;

import io.musician101.manualmending.common.Config;
import java.io.IOException;

public class SpigotConfig implements Config {

    public SpigotConfig() {

    }

    @Override
    public boolean repairAll() {
        return SpigotManualMending.instance().getConfig().getBoolean("repairAll");
    }

    @Override
    public void load() throws IOException {
        SpigotManualMending.instance().saveDefaultConfig();
    }
}
