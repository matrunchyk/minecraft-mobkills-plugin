package com.matrunchyk.mobkillsplugin;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HappyBirthdayCommand implements CommandExecutor {

    private final MobKillsPlugin plugin;

    public HappyBirthdayCommand(MobKillsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (!player.hasPermission("mobkills.happybirthday")) {
                player.sendMessage(Component.text(ChatColor.RED + "You do not have permission to use this command."));
                return true;
            }

            plugin.wishHappyBirthday(player, player);
            return true;
        }

        sender.sendMessage(Component.text(ChatColor.RED + "This command can only be executed by a player."));

        return true;
    }
}
