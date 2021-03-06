package io.musician101.manualmending.forge;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.musician101.manualmending.common.ManualMending;
import io.musician101.manualmending.common.Reference;
import io.musician101.manualmending.common.Reference.Commands;
import io.musician101.manualmending.common.Reference.Messages;
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
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Reference.ID)
public class ForgeManualMending extends ManualMending<LiteralArgumentBuilder<CommandSource>, RegisterCommandsEvent, ItemStack, PlayerEntity> {

    private final Logger logger = LogManager.getLogger(Reference.ID);

    public ForgeManualMending() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::forgePreInit);
        MinecraftForge.EVENT_BUS.addListener(this::forgeServerStart);
        config = new ForgeConfig();
    }

    private void forgePreInit(FMLCommonSetupEvent event) {
        try {
            config.load();
        }
        catch (IOException e) {
            logger.error(String.format(Messages.CONFIG_LOAD_ERROR, e.getMessage()), e);
        }
    }

    private void forgeServerStart(FMLServerStartingEvent event) {
        MinecraftForge.EVENT_BUS.addListener(this::onPickUpXP);
    }

    @Nonnull
    @Override
    protected LiteralArgumentBuilder<CommandSource> mendAll() {
        return LiteralArgumentBuilder.<CommandSource>literal(Commands.ALL).executes(context -> {
            ServerPlayerEntity player = context.getSource().asPlayer();
            player.sendMessage(new StringTextComponent(Messages.MENDING_ITEM).mergeStyle(TextFormatting.GREEN), Util.DUMMY_UUID);
            player.experienceTotal = mendItems(player.experienceTotal, player);
            player.sendMessage(new StringTextComponent(Messages.MENDING_COMPLETE).mergeStyle(TextFormatting.GREEN), Util.DUMMY_UUID);
            return 1;
        });
    }

    @Override
    protected int mendItem(int xp, @Nonnull ItemStack itemStack) {
        float ratio = itemStack.getItem().getXpRepairRatio(itemStack);
        int i = Math.min(roundAverage(xp * ratio), itemStack.getDamage());
        itemStack.setDamage(itemStack.getDamage() - i);
        return xp - roundAverage(i / ratio);
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

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        registerCommand(event);
    }

    private void onPickUpXP(PickupXp event) {
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

    @Override
    protected void registerCommand(RegisterCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSource>literal(Commands.MEND).executes(context -> {
            ServerPlayerEntity player = context.getSource().asPlayer();
            player.sendMessage(new StringTextComponent(Messages.MENDING_ITEMS).mergeStyle(TextFormatting.GREEN), Util.DUMMY_UUID);
            ItemStack itemStack = player.getHeldItemMainhand();
            if (!itemStack.isEmpty() && itemStack.isDamaged() && EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, itemStack) > 0) {
                player.experienceTotal = mendItem(player.experienceTotal, itemStack);
            }

            player.sendMessage(new StringTextComponent(Messages.MENDING_COMPLETE).mergeStyle(TextFormatting.GREEN), Util.DUMMY_UUID);
            return 1;
        }));
    }

    @Nonnull
    @Override
    protected LiteralArgumentBuilder<CommandSource> reload() {
        return LiteralArgumentBuilder.<CommandSource>literal(Commands.RELOAD).requires(source -> source.hasPermissionLevel(4)).executes(context -> {
            try {
                getConfig().load();
                context.getSource().sendFeedback(new StringTextComponent(Messages.CONFIG_RELOADED).mergeStyle(TextFormatting.GREEN), false);
            }
            catch (IOException e) {
                context.getSource().sendFeedback(new StringTextComponent(String.format(Messages.CONFIG_LOAD_ERROR, e.getMessage())).mergeStyle(TextFormatting.RED), false);
            }

            return 1;
        });
    }
}
