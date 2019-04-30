package io.musician101.manualmending.sponge;

import com.google.inject.Inject;
import io.musician101.manualmending.common.Config;
import io.musician101.manualmending.common.ManualMending;
import io.musician101.manualmending.common.Reference;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.item.UseLimitProperty;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tuple;

@Plugin(id = Reference.MOD_ID, name = "ManualMending", version = "1.0-SNAPSHOT", authors = "Musician101", description = "Improved behavior for the vanilla Mending enchantment.")
public final class SpongeManualMending extends ManualMending<Tuple<CommandSpec, String>, Tuple<ItemStack, Consumer<ItemStack>>, Player> {

    @Inject
    private Logger logger;

    public static SpongeManualMending instance() {
        return Sponge.getPluginManager().getPlugin(Reference.MOD_ID).flatMap(PluginContainer::getInstance).filter(SpongeManualMending.class::isInstance).map(SpongeManualMending.class::cast).orElseThrow(() -> new IllegalStateException("ManualMending is not enabled."));
    }

    @Listener
    public void preInit(GamePreInitializationEvent event) {
        preInit(new Config(new File("config", Reference.MOD_ID)), e -> logger.error("Failed to load config.", e));
    }

    @Listener
    public void serverStart(GameStartingServerEvent event) {
        serverStart();
    }

    @Override
    protected void registerCommand(@Nonnull Tuple<CommandSpec, String> command) {
        Sponge.getCommandManager().register(this, command.getFirst(), command.getSecond());
    }

    @Nonnull
    @Override
    protected Tuple<CommandSpec, String> mend() {
        return Tuple.of(CommandSpec.builder().permission("manualmending.mend").executor((src, args) -> {
            if (!(src instanceof Player)) {
                src.sendMessage(Text.of(TextColors.RED, "Only players can run this commands."));
                return CommandResult.empty();
            }

            if (!src.hasPermission(Reference.MOD_ID + ".mend")) {
                src.sendMessage(Text.of(TextColors.RED, "You do not have permission for this command."));
                return CommandResult.empty();
            }

            Player player = (Player) src;
            player.sendMessage(Text.of(TextColors.GREEN, "[ManualMending] Mending item..."));
            player.getItemInHand(HandTypes.MAIN_HAND).ifPresent(itemStack -> player.get(Keys.TOTAL_EXPERIENCE).ifPresent(xp -> player.offer(Keys.TOTAL_EXPERIENCE, mendItem(xp, new Tuple<>(itemStack, is -> player.setItemInHand(HandTypes.MAIN_HAND, is))))));
            player.sendMessage(Text.of(TextColors.GREEN, "[ManualMending] Mending complete."));
            return CommandResult.success();
        }).build(), "mend");
    }

    @Nonnull
    @Override
    protected Tuple<CommandSpec, String> mendAll() {
        return Tuple.of(CommandSpec.builder().permission("manualmending.all").executor((src, args) -> {
            if (!(src instanceof Player)) {
                src.sendMessage(Text.of(TextColors.RED, "Only players can run this commands."));
                return CommandResult.empty();
            }

            if (!src.hasPermission(Reference.MOD_ID + ".all")) {
                src.sendMessage(Text.of(TextColors.RED, "You do not have permission for this command."));
                return CommandResult.empty();
            }

            Player player = (Player) src;
            player.sendMessage(Text.of(TextColors.GREEN, "[ManualMending] Mending items..."));
            player.get(Keys.TOTAL_EXPERIENCE).ifPresent(xp -> player.offer(Keys.TOTAL_EXPERIENCE, mendItems(xp, player)));
            player.sendMessage(Text.of(TextColors.GREEN, "[ManualMending] Mending complete."));
            return CommandResult.success();
        }).build(), "mendall");
    }

    @Nonnull
    @Override
    protected Tuple<CommandSpec, String> reload() {
        return Tuple.of(CommandSpec.builder().permission("manualmending.reload").executor((src, args) -> {
            if (!src.hasPermission(Reference.MOD_ID + ".reload")) {
                src.sendMessage(Text.of(TextColors.RED, "You do not have permission for this command."));
                return CommandResult.empty();
            }

            try {
                getConfig().load();
                src.sendMessage(Text.of(TextColors.GREEN, "Config reloaded."));
            }
            catch (IOException e) {
                src.sendMessage(Text.of(TextColors.RED, "An error occurred while attempting to reload the config."));
                logger.error("An error occurred while attempting to reload the config.");
                e.printStackTrace();
            }

            return CommandResult.success();
        }).build(), "mendreload");
    }

    @Override
    protected int mendItem(int xp, @Nonnull Tuple<ItemStack, Consumer<ItemStack>> itemStack) {
        ItemStack is = itemStack.getFirst();
        Optional<Integer> durabilityOptional = is.get(Keys.ITEM_DURABILITY);
        if (!durabilityOptional.isPresent()) {
            return xp;
        }

        Optional<UseLimitProperty> useLimitProperty = is.getProperty(UseLimitProperty.class);
        if (!useLimitProperty.isPresent()) {
            return xp;
        }

        int maxDurability = useLimitProperty.get().getValue();
        int durability = durabilityOptional.get();
        float ratio = 2f;
        int i = Math.min(roundAverage(xp * ratio), maxDurability - durability);
        is.offer(Keys.ITEM_DURABILITY, durability + i);
        itemStack.getSecond().accept(is);
        return xp - roundAverage(i / ratio);
    }

    @Override
    public int mendItems(int xp, @Nonnull Player player) {
        if (xp <= 0) {
            return 0;
        }

        PlayerInventory inv = (PlayerInventory) player.getInventory();
        Map<ItemStack, Consumer<ItemStack>> items = new HashMap<>();
        if (getConfig().repairAll()) {
            inv.slots().forEach(slot -> slot.peek().filter(itemStack -> itemStack.getProperty(UseLimitProperty.class).isPresent()).ifPresent(itemStack -> items.put(itemStack, slot::set)));
        }
        else {
            player.getHelmet().ifPresent(itemStack -> items.put(itemStack, player::setHelmet));
            player.getChestplate().ifPresent(itemStack -> items.put(itemStack, player::setChestplate));
            player.getLeggings().ifPresent(itemStack -> items.put(itemStack, player::setLeggings));
            player.getBoots().ifPresent(itemStack -> items.put(itemStack, player::setBoots));
            player.getItemInHand(HandTypes.MAIN_HAND).ifPresent(itemStack -> items.put(itemStack, is -> player.setItemInHand(HandTypes.MAIN_HAND, is)));
            player.getItemInHand(HandTypes.OFF_HAND).ifPresent(itemStack -> items.put(itemStack, is -> player.setItemInHand(HandTypes.OFF_HAND, is)));
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
