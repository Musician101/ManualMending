package io.musician101.manualmending.sponge.mixin;

import io.musician101.manualmending.sponge.SpongeManualMending;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = ExperienceOrb.class, priority = 1001)
public abstract class MixinEntityXpOrb extends MixinEntity {

    @Shadow(remap = false)
    private int value;

    @Inject(method = "playerTouch", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getRandomEquippedWithEnchantment(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/item/ItemStack;)Ljava/util/Map$Entry;", ordinal = 0), locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true, remap = false)
    public void mend(Player player, CallbackInfo ci) {
        SpongeManualMending plugin = SpongeManualMending.instance();
        if (plugin.getConfig().repairAll()) {
            value = plugin.mendItems(value, (ServerPlayer) player);
            if (value > 0) {
                player.giveExperiencePoints(value);
            }

            kill();
            return;
        }
    }
}
