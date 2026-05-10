package me.dru.showcase;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.dru.showcase.block.Showcase;

public class Commands implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length >= 2 && args[0].equalsIgnoreCase("unstuck")) {
			if (!sender.hasPermission("modernshowcase.admin")) {
				sender.sendMessage(ChatColor.RED + "No permission.");
				return true;
			}

			Player target = Bukkit.getPlayerExact(args[1]);
			if (target == null) {
				sender.sendMessage(ChatColor.RED + "Player not found.");
				return true;
			}

			ShowcaseUI.forceClear(target);
			sender.sendMessage(ChatColor.GREEN + "Cleared showcase preview state for " + target.getName() + ".");
			target.sendMessage(ChatColor.GREEN + "Your showcase preview state has been cleared.");
			return true;
		}
        return false;
    }

}
