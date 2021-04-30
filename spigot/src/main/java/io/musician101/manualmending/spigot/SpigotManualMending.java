package io.musician101.manualmending.spigot;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.musician101.bukkitier.Bukkitier;
import io.musician101.manualmending.common.ManualMending;
import io.musician101.manualmending.common.Reference.Commands;
import io.musician101.manualmending.common.Reference.Messages;
import io.musician101.manualmending.common.Reference.Permissions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotManualMending extends JavaPlugin implements Listener {

    private final ManualMendingWrapper wrapper = new ManualMendingWrapper();

    public static SpigotManualMending instance() {
        return getPlugin(SpigotManualMending.class);
    }

    @Override
    public void onEnable() {
        wrapper.preInit();
        wrapper.registerCommand(null);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPickUpXP(PlayerExpChangeEvent event) {
        if (wrapper.getConfig().repairAll()) {
            Player player = event.getPlayer();
            int xp = wrapper.mendItems(event.getAmount(), player);
            if (xp > 0) {
                player.giveExp(xp);
            }
        }
    }

    private class ManualMendingWrapper extends ManualMending<LiteralArgumentBuilder<CommandSender>, Object, ItemStack, Player> {

        public void preInit() {
            this.config = new SpigotConfig();
            try {
                this.config.load();
            }
            catch (IOException e) {
                getLogger().warning(String.format(Messages.CONFIG_LOAD_ERROR, e.getMessage()));
            }
        }

        @Override
        protected void registerCommand(Object event) {
            Bukkitier.registerCommand(SpigotManualMending.instance(), Bukkitier.literal(Commands.MEND).requires(sender -> sender.hasPermission(Permissions.MEND) && sender instanceof Player).executes(context -> {
                Player player = (Player) context.getSource();
                player.sendMessage(ChatColor.GREEN + Messages.MENDING_ITEM);
                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack.getType() != Material.AIR && itemStack.hasItemMeta() && itemStack.getItemMeta() instanceof Damageable && itemStack.getEnchantmentLevel(Enchantment.MENDING) > 0) {
                    player.setTotalExperience(mendItem(player.getTotalExperience(), itemStack));
                }

                player.sendMessage(ChatColor.GREEN + Messages.MENDING_COMPLETE);
                return 1;
            }).then(mendAll()).then(reload()));
        }

        @Nonnull
        @Override
        protected LiteralArgumentBuilder<CommandSender> mendAll() {
            return Bukkitier.literal(Commands.ALL).requires(sender -> sender.hasPermission(Permissions.MEND_ALL) && sender instanceof Player).executes(context -> {
                Player player = (Player) context.getSource();
                player.sendMessage(ChatColor.GREEN + Messages.MENDING_ITEMS);
                player.setTotalExperience(mendItems(player.getTotalExperience(), player));
                player.sendMessage(ChatColor.GREEN + Messages.MENDING_COMPLETE);
                return 1;
            });
        }

        @Nonnull
        @Override
        protected LiteralArgumentBuilder<CommandSender> reload() {
            return Bukkitier.literal(Commands.RELOAD).requires(sender -> sender.hasPermission(Permissions.RELOAD)).executes(context -> {
                try {
                    getConfig().load();
                    context.getSource().sendMessage(ChatColor.GREEN + "Config reloaded.");
                }
                catch (IOException e) {
                    context.getSource().sendMessage(ChatColor.RED + String.format(Messages.CONFIG_LOAD_ERROR, e.getMessage()));
                }

                return 1;
            });
        }

        @Override
        public int mendItems(int xp, @Nonnull Player player) {
            if (xp <= 0) {
                return 0;
            }

            PlayerInventory inv = player.getInventory();
            List<ItemStack> items = new ArrayList<>();
            if (getConfig().repairAll()) {
                items.addAll(Arrays.asList(inv.getContents()));
            }
            else {
                items.addAll(Arrays.asList(inv.getArmorContents()));
                items.add(inv.getItemInMainHand());
                items.add(inv.getItemInOffHand());
            }

            items.removeIf(itemStack -> itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta() || !(itemStack.getItemMeta() instanceof Damageable) || itemStack.getEnchantmentLevel(Enchantment.MENDING) == 0);
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
            Damageable meta = (Damageable) itemStack.getItemMeta();
            float ratio = 2f;
            int i = Math.min(roundAverage(xp * ratio), meta.getDamage());
            meta.setDamage(meta.getDamage() - i);
            itemStack.setItemMeta((ItemMeta) meta);
            return xp - roundAverage(i / ratio);
        }
    }
}
