package io.musician101.manualmending.common;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public abstract class ManualMending<C, E, I, P> {

    protected Config config;

    @Nonnull
    public final Config getConfig() {
        return config;
    }

    protected abstract void registerCommand(E event);

    @Nonnull
    protected abstract C mendAll();

    @Nonnull
    protected abstract C reload();

    protected final int roundAverage(@Nonnegative float value) {
        double floor = Math.floor(value);
        return (int) (floor + (Math.random() < value - floor ? 1 : 0));
    }

    @Nonnegative
    public abstract int mendItems(@Nonnegative int xp, @Nonnull P player);

    @Nonnegative
    protected abstract int mendItem(@Nonnegative int xp, @Nonnull I itemStack);
}
