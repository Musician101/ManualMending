package io.musician101.manualmending.sponge.mixin;

import io.musician101.manualmending.sponge.SpongeManualMending;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = EntityXPOrb.class, priority = 1001)
public abstract class MixinEntityXpOrb extends MixinEntity {

    @Shadow private int xpValue;

    @Inject(method = "onCollideWithPlayer", at = @At(value = "INVOKE_ASSIGN", target =
            "Lnet/minecraft/enchantment/EnchantmentHelper;getEnchantedItem(" +
                    "Lnet/minecraft/enchantment/Enchantment;" +
                    "Lnet/minecraft/entity/EntityLivingBase;)" +
                    "Lnet/minecraft/item/ItemStack;", ordinal = 0), locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
    public void mend(EntityPlayer player, CallbackInfo ci) {
        SpongeManualMending plugin = SpongeManualMending.instance();
        if (plugin.getConfig().repairAll()) {
            xpValue = plugin.mendItems(xpValue, (Player) player);
            if (xpValue > 0) {
                player.addExperience(xpValue);
            }

            setDead();
            return;
        }
    }
}
