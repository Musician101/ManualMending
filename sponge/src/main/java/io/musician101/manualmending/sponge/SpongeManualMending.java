package io.musician101.manualmending.sponge;

import com.google.inject.Inject;
import io.musician101.manualmending.common.ManualMending;
import io.musician101.manualmending.common.Reference;
import io.musician101.manualmending.common.Reference.Commands;
import io.musician101.manualmending.common.Reference.Messages;
import io.musician101.manualmending.common.Reference.Permissions;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.Command.Parameterized;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;

@Plugin(Reference.ID)
public final class SpongeManualMending extends ManualMending<Parameterized, RegisterCommandEvent<Parameterized>, Tuple<ItemStack, Consumer<ItemStack>>, ServerPlayer> {

    @Nonnull
    private final PluginContainer pluginContainer;

    @Inject
    public SpongeManualMending(@Nonnull PluginContainer pluginContainer, @DefaultConfig(sharedRoot = true) ConfigurationReference<ConfigurationNode> configReference) {
        this.pluginContainer = pluginContainer;
        config = new SpongeConfig(configReference);
    }

    public static SpongeManualMending instance() {
        return Sponge.pluginManager().plugin(Reference.ID).map(PluginContainer::getInstance).filter(SpongeManualMending.class::isInstance).map(SpongeManualMending.class::cast).orElseThrow(() -> new IllegalStateException("ManualMending is not enabled."));
    }

    @Listener
    public void constructPlugin(ConstructPluginEvent event) {
        try {
            config.load();
        }
        catch (IOException e) {
            pluginContainer.getLogger().error("Failed to load config.", e);
        }
    }

    @Nonnull
    public PluginContainer getPluginContainer() {
        return pluginContainer;
    }

    @Listener
    public void onRegisterCommand(RegisterCommandEvent<Parameterized> event) {
        registerCommand(event);
    }

    @Override
    protected void registerCommand(@Nonnull RegisterCommandEvent<Parameterized> event) {
        event.register(pluginContainer, Command.builder().executionRequirements(commandCause -> commandCause.subject() instanceof ServerPlayer && commandCause.hasPermission(Permissions.MEND)).executor(context -> {
            ServerPlayer player = (ServerPlayer) context.subject();
            player.sendMessage(Component.text(Messages.MENDING_ITEM).color(NamedTextColor.GREEN));
            ItemStack itemStack = player.itemInHand(HandTypes.MAIN_HAND);
            player.get(Keys.EXPERIENCE).ifPresent(xp -> player.offer(Keys.EXPERIENCE, mendItem(xp, new Tuple<>(itemStack, is -> player.setItemInHand(HandTypes.MAIN_HAND, is)))));
            player.sendMessage(Component.text(Messages.MENDING_COMPLETE).color(NamedTextColor.GREEN));
            return CommandResult.success();
        }).addChild(mendAll(), Commands.ALL).addChild(reload(), Commands.RELOAD).build(), Commands.MEND);
    }

    @Nonnull
    @Override
    protected Parameterized mendAll() {
        return Command.builder().executionRequirements(commandCause -> commandCause.subject() instanceof ServerPlayer && commandCause.hasPermission(Permissions.MEND_ALL)).executor(context -> {
            ServerPlayer player = (ServerPlayer) context.subject();
            player.sendMessage(Component.text(Messages.MENDING_ITEMS).color(NamedTextColor.GREEN));
            player.get(Keys.EXPERIENCE).ifPresent(xp -> player.offer(Keys.EXPERIENCE, mendItems(xp, player)));
            player.sendMessage(Component.text(Messages.MENDING_COMPLETE).color(NamedTextColor.GREEN));
            return CommandResult.success();
        }).build();
    }

    @Nonnull
    @Override
    protected Parameterized reload() {
        return Command.builder().permission(Permissions.RELOAD).executor(context -> {
            try {
                config.load();
                context.sendMessage(Identity.nil(), Component.text(Messages.CONFIG_RELOADED).color(NamedTextColor.GREEN));
            }
            catch (IOException e) {
                context.sendMessage(Identity.nil(), Component.text(String.format(Messages.CONFIG_LOAD_ERROR, e.getMessage())).color(NamedTextColor.RED));
            }

            return CommandResult.success();
        }).build();
    }

    @Override
    protected int mendItem(int xp, @Nonnull Tuple<ItemStack, Consumer<ItemStack>> itemStack) {
        ItemStack is = itemStack.first();
        Optional<Integer> durabilityOptional = is.get(Keys.ITEM_DURABILITY);
        if (!durabilityOptional.isPresent()) {
            return xp;
        }


        Optional<Integer> maxDurabilityOptional = is.get(Keys.MAX_DURABILITY);
        if (!maxDurabilityOptional.isPresent()) {
            return xp;
        }

        int maxDurability = maxDurabilityOptional.get();
        int durability = durabilityOptional.get();
        float ratio = 2f;
        int i = Math.min(roundAverage(xp * ratio), maxDurability - durability);
        is.offer(Keys.ITEM_DURABILITY, durability + i);
        itemStack.second().accept(is);
        return xp - roundAverage(i / ratio);
    }

    @Override
    public int mendItems(int xp, @Nonnull ServerPlayer player) {
        if (xp <= 0) {
            return 0;
        }

        PlayerInventory inv = player.inventory();
        Map<ItemStack, Consumer<ItemStack>> items = new HashMap<>();
        if (getConfig().repairAll()) {
            inv.slots().forEach(slot -> {
                ItemStack itemStack = slot.peek();
                itemStack.get(Keys.MAX_DURABILITY).ifPresent(maxDurability -> items.put(itemStack, slot::set));
            });
        }
        else {
            EquipmentInventory equipment = player.equipment();
            equipment.slot(EquipmentTypes.HEAD).ifPresent(slot -> items.put(slot.peek(), slot::set));
            equipment.slot(EquipmentTypes.CHEST).ifPresent(slot -> items.put(slot.peek(), slot::set));
            equipment.slot(EquipmentTypes.LEGS).ifPresent(slot -> items.put(slot.peek(), slot::set));
            equipment.slot(EquipmentTypes.FEET).ifPresent(slot -> items.put(slot.peek(), slot::set));
            items.put(player.itemInHand(HandTypes.MAIN_HAND), is -> player.setItemInHand(HandTypes.MAIN_HAND, is));
            items.put(player.itemInHand(HandTypes.OFF_HAND), is -> player.setItemInHand(HandTypes.OFF_HAND, is));
        }

        for (Entry<ItemStack, Consumer<ItemStack>> entry : items.entrySet()) {
            xp = mendItem(xp, Tuple.of(entry.getKey(), entry.getValue()));
            if (xp == 0) {
                break;
            }
        }

        return xp;
    }
}
