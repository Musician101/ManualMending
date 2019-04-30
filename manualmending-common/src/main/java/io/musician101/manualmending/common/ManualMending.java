package io.musician101.manualmending.common;

import java.io.IOException;
import java.util.function.Consumer;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public abstract class ManualMending<C, I, P> {

    private Config config;

    @Nonnull
    public final Config getConfig() {
        return config;
    }

    @Nonnull
    protected abstract C mend();

    @Nonnull
    protected abstract C mendAll();

    @Nonnull
    protected abstract C reload();

    protected abstract void registerCommand(@Nonnull C command);

    public final void preInit(@Nonnull Config config, @Nonnull Consumer<IOException> logger) {
        this.config = config;
        try {
            this.config.load();
        }
        catch (IOException e) {
            logger.accept(e);
        }
    }

    public final void serverStart() {
        registerCommand(mend());
        registerCommand(mendAll());
        registerCommand(reload());
    }

    protected final int roundAverage(@Nonnegative float value) {
        double floor = Math.floor(value);
        return (int) (floor + (Math.random() < value - floor ? 1 : 0));
    }

    @Nonnegative
    public abstract int mendItems(@Nonnegative int xp, @Nonnull P player);

    @Nonnegative
    protected abstract int mendItem(@Nonnegative int xp, @Nonnull I itemStack);
}
