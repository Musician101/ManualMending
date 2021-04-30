package io.musician101.manualmending.sponge;

import io.musician101.manualmending.common.Config;
import javax.annotation.Nonnull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.ValueReference;

public class SpongeConfig implements Config {

    @Nonnull
    private final ConfigurationReference<ConfigurationNode> configReference;
    private ValueReference<DefaultConfig, ConfigurationNode> config;

    @Override
    public boolean repairAll() {
        return config.get().repairAll;
    }

    public SpongeConfig(@Nonnull ConfigurationReference<ConfigurationNode> configReference) {
        this.configReference = configReference;
    }

    @Override
    public void load() throws ConfigurateException {
        config = configReference.referenceTo(DefaultConfig.class);
        configReference.save();
    }

    @ConfigSerializable
    public static class DefaultConfig {

        @Setting
        boolean repairAll = false;
    }
}
