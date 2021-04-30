package io.musician101.manualmending.sponge.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Entity.class, priority = 1001)
public abstract class MixinEntity {

    @Shadow
    public abstract void kill();
}
