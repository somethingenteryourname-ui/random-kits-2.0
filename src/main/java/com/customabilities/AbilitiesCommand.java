package com.customabilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AbilitiesCommand implements CommandExecutor, TabCompleter {

    private final CustomAbilities plugin;

    public AbilitiesCommand(CustomAbilities plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "CustomAbilities config reloaded.");
            return true;
        }

        if (!args[0].equalsIgnoreCase("give") || args.length < 3) {
            sendUsage(sender, label);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                return true;
            }
        }

        List<AbilityType> toGive = new ArrayList<>();
        if (args[2].equalsIgnoreCase("all")) {
            toGive.addAll(List.of(AbilityType.values()));
        } else {
            AbilityType type = AbilityType.fromId(args[2]);
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Unknown item. Options: " + String.join(", ", itemIds()) + ", all");
                return true;
            }
            toGive.add(type);
        }

        for (AbilityType type : toGive) {
            ItemStack item = plugin.items().create(type, amount);
            Map<Integer, ItemStack> leftover = target.getInventory().addItem(item);
            for (ItemStack extra : leftover.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), extra);
            }
        }
        sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " " + args[2].toLowerCase() + ".");
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " give <player> <item|all> [amount]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
        sender.sendMessage(ChatColor.GRAY + "Items: " + String.join(", ", itemIds()));
    }

    private List<String> itemIds() {
        List<String> ids = new ArrayList<>();
        for (AbilityType type : AbilityType.values()) {
            ids.add(type.id());
        }
        return ids;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("give");
            options.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                options.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            options.addAll(itemIds());
            options.add("all");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            options.add("1");
            options.add("16");
            options.add("64");
        }
        String typed = args[args.length - 1].toLowerCase();
        options.removeIf(option -> !option.toLowerCase().startsWith(typed));
        return options;
    }
}
