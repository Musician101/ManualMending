package io.musician101.manualmending.spigot;

import io.musician101.manualmending.common.Config;
import io.musician101.manualmending.common.ManualMending;
import io.musician101.manualmending.common.Reference;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
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

    @Override
    public void onEnable() {
        wrapper.preInit(new Config(new File("plugins", Reference.MOD_ID)), e -> {
            getLogger().severe("Failed to load config.");
            e.printStackTrace();
        });
        wrapper.serverStart();
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

    private class ManualMendingWrapper extends ManualMending<Entry<CommandExecutor, String>, ItemStack, Player> {

        @Nonnull
        @Override
        protected Entry<CommandExecutor, String> mend() {
            return new SimpleEntry<>((sender, command, label, args) -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can run this commands.");
                    return false;
                }

                if (!sender.hasPermission(Reference.MOD_ID + ".mend")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission for this command.");
                    return false;
                }

                Player player = (Player) sender;
                player.sendMessage(ChatColor.GREEN + "[ManualMending] Mending item...");
                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack.getType() != Material.AIR && itemStack.hasItemMeta() && itemStack.getItemMeta() instanceof Damageable && itemStack.getEnchantmentLevel(Enchantment.MENDING) > 0) {
                    player.setTotalExperience(mendItem(player.getTotalExperience(), itemStack));
                }

                player.sendMessage(ChatColor.GREEN + "[ManualMending] Mending complete.");
                return true;
            }, "mend");
        }

        @Nonnull
        @Override
        protected Entry<CommandExecutor, String> mendAll() {
            return new SimpleEntry<>((sender, command, label, args) -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can run this commands.");
                    return false;
                }

                if (!sender.hasPermission(Reference.MOD_ID + ".mend")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission for this command.");
                    return false;
                }

                Player player = (Player) sender;
                player.sendMessage(ChatColor.GREEN + "[ManualMending] Mending items...");
                player.setTotalExperience(mendItems(player.getTotalExperience(), player));
                player.sendMessage(ChatColor.GREEN + "[ManualMending] Mending complete.");
                return true;
            }, "mendAll");
        }

        @Nonnull
        @Override
        protected Entry<CommandExecutor, String> reload() {
            return new SimpleEntry<>((sender, command, label, args) -> {
                if (!sender.hasPermission(Reference.MOD_ID + ".reload")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission for this command.");
                    return false;
                }

                try {
                    getConfig().load();
                    sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                }
                catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "An error occurred while attempting to reload the config.");
                    getLogger().severe("An error occurred while attempting to reload the config.");
                    e.printStackTrace();
                }

                return true;
            }, "mendReload");
        }

        @Override
        protected void registerCommand(@Nonnull Entry<CommandExecutor, String> command) {
            Bukkit.getPluginCommand(command.getValue()).setExecutor(command.getKey());
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
