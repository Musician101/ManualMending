package io.musician101.manualmending.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.musician101.manualmending.common.Config;
import io.musician101.manualmending.common.ManualMending;
import io.musician101.manualmending.common.Reference;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Reference.MOD_ID)
public class ForgeManualMending extends ManualMending<LiteralArgumentBuilder<CommandSource>, ItemStack, PlayerEntity> {

    private final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    public ForgeManualMending() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::forgePreInit);
        MinecraftForge.EVENT_BUS.addListener(this::forgeServerStart);
    }

    private void forgePreInit(FMLCommonSetupEvent event) {
        preInit(new Config(new File("config", Reference.MOD_ID)), e -> logger.error("Failed to load config.", e));
    }

    private void forgeServerStart(FMLServerStartingEvent event) {
        serverStart();
        MinecraftForge.EVENT_BUS.addListener(this::onPickUpXP);
    }

    @Nonnull
    @Override
    protected LiteralArgumentBuilder<CommandSource> mend() {
        return LiteralArgumentBuilder.<CommandSource>literal("mend").executes(context -> {
            ServerPlayerEntity player = context.getSource().asPlayer();
            player.sendMessage(new StringTextComponent("[ManualMending] Mending item...").setStyle(new Style().setColor(TextFormatting.GREEN)));
            ItemStack itemStack = player.getHeldItemMainhand();
            if (!itemStack.isEmpty() && itemStack.isDamaged() && EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, itemStack) > 0) {
                player.experienceTotal = mendItem(player.experienceTotal, itemStack);
            }

            player.sendMessage(new StringTextComponent("[ManualMending] Mending complete.").setStyle(new Style().setColor(TextFormatting.GREEN)));
            return 1;
        });
    }

    @Nonnull
    @Override
    protected LiteralArgumentBuilder<CommandSource> mendAll() {
        return LiteralArgumentBuilder.<CommandSource>literal("mendAll").executes(context -> {
            ServerPlayerEntity player = context.getSource().asPlayer();
            player.sendMessage(new StringTextComponent("[ManualMending] Mending items...").setStyle(new Style().setColor(TextFormatting.GREEN)));
            player.experienceTotal = mendItems(player.experienceTotal, player);
            player.sendMessage(new StringTextComponent("[ManualMending] Mending complete.").setStyle(new Style().setColor(TextFormatting.GREEN)));
            return 1;
        });
    }

    @Nonnull
    @Override
    protected LiteralArgumentBuilder<CommandSource> reload() {
        return LiteralArgumentBuilder.<CommandSource>literal("mendReload").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
            try {
                getConfig().load();
                context.getSource().sendFeedback(new StringTextComponent("Config reloaded.").setStyle(new Style().setColor(TextFormatting.GREEN)), false);
            }
            catch (IOException e) {
                context.getSource().sendFeedback(new StringTextComponent("An error occurred while attempting to reload the config.").setStyle(new Style().setColor(TextFormatting.RED)), false);
                logger.error("An error occurred while attempting to reload the config.", e);
            }

            return 1;
        });
    }

    @Override
    protected void registerCommand(@Nonnull LiteralArgumentBuilder<CommandSource> command) {
        CommandDispatcher<CommandSource> dispatcher = LogicalSidedProvider.INSTANCE.<MinecraftServer>get(LogicalSide.SERVER).getCommandManager().getDispatcher();
        dispatcher.register(mend());
        dispatcher.register(mendAll());
        dispatcher.register(reload());
    }

    @Override
    public int mendItems(int xp, @Nonnull PlayerEntity player) {
        if (xp <= 0) {
            return 0;
        }

        PlayerInventory inv = player.inventory;
        List<ItemStack> items = new ArrayList<>();
        if (getConfig().repairAll()) {
            items.addAll(inv.mainInventory);
            items.addAll(inv.armorInventory);
            items.addAll(inv.offHandInventory);
        }
        else {
            items.addAll(Enchantments.MENDING.getEntityEquipment(player).values());
        }

        items.removeIf(itemStack -> itemStack.isEmpty() || !itemStack.isDamaged() || EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, itemStack) == 0);
        for (ItemStack itemStack : items) {
            xp = mendItem(xp, itemStack);
            if (xp == 0) {
                break;
            }
        }

        return xp;
    }

    @Override
    protected int mendItem(int xp, @Nonnull ItemStack itemStack) {
        float ratio = itemStack.getItem().getXpRepairRatio(itemStack);
        int i = Math.min(roundAverage(xp * ratio), itemStack.getDamage());
        itemStack.setDamage(itemStack.getDamage() - i);
        return xp - roundAverage(i / ratio);
    }

    private void onPickUpXP(PlayerPickupXpEvent event) {
        if (getConfig().repairAll()) {
            event.setCanceled(true);
            ExperienceOrbEntity xpOrb = event.getOrb();
            PlayerEntity player = event.getPlayer();
            player.xpCooldown = 2;
            player.onItemPickup(xpOrb, 1);
            int xp = mendItems(xpOrb.getXpValue(), player);
            if (xp > 0) {
                player.giveExperiencePoints(xp);
            }

            xpOrb.remove();
        }
    }
}
