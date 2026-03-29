package com.lavishmc.bountySystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BountyCommand implements CommandExecutor, TabCompleter {

    private final BountyManager bountyManager;

    public BountyCommand(BountyManager bountyManager) {
        this.bountyManager = bountyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        // /bounty — open GUI (placeholder)
        if (args.length == 0) {
            player.sendMessage(msg("&eGUI coming soon."));
            return true;
        }

        // /bounty place <player> <amount>
        if (args[0].equalsIgnoreCase("place")) {
            if (args.length < 3) {
                player.sendMessage(msg("&cUsage: /bounty place <player> <amount>"));
                return true;
            }
            return handlePlace(player, args[1], args[2]);
        }

        player.sendMessage(msg("&cUnknown sub-command. Usage: /bounty [place <player> <amount>]"));
        return true;
    }

    private boolean handlePlace(Player placer, String targetName, String amountStr) {
        Economy economy = BountySystem.getEconomy();
        if (economy == null) {
            placer.sendMessage(msg("&cEconomy is not available. Cannot place bounties."));
            return true;
        }

        // Parse amount.
        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            placer.sendMessage(msg("&cAmount must be a positive whole number."));
            return true;
        }
        if (amount <= 0) {
            placer.sendMessage(msg("&cAmount must be a positive number."));
            return true;
        }

        // Resolve target — prefer online player, fall back to offline.
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        OfflinePlayer target = (onlineTarget != null)
                ? onlineTarget
                : Bukkit.getOfflinePlayer(targetName);

        // Ensure the target has actually played on this server.
        if (!target.hasPlayedBefore() && onlineTarget == null) {
            placer.sendMessage(msg("&cPlayer &e" + targetName + " &cwas not found."));
            return true;
        }

        // Prevent self-bounty.
        if (target.getUniqueId().equals(placer.getUniqueId())) {
            placer.sendMessage(msg("&cYou cannot place a bounty on yourself."));
            return true;
        }

        // Check balance.
        if (!economy.has(placer, (double) amount)) {
            placer.sendMessage(msg("&cYou do not have enough money to place a &e$" + amount + " &cbounty."));
            return true;
        }

        // Deduct and place.
        economy.withdrawPlayer(placer, (double) amount);
        bountyManager.placeBounty(target.getUniqueId(), amount);

        String displayName = target.getName() != null ? target.getName() : targetName;
        placer.sendMessage(msg("&aYou placed a &e$" + amount + " &abounty on &e" + displayName + "&a!"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("place");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("place")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        return completions;
    }

    private static Component msg(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
    }
}
