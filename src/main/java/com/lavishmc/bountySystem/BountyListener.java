package com.lavishmc.bountysystem;

import com.lavishmc.bountysystem.BountyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class BountyListener implements Listener {

    /** PDC key written by HeadHunter onto player-head drops. */
    private static final NamespacedKey HEAD_OWNER_KEY =
            new NamespacedKey("headhunter", "player_head_owner");

    private final BountySystem plugin;
    private final BountyManager bountyManager;

    public BountyListener(BountySystem plugin, BountyManager bountyManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only process main-hand right-clicks.
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player seller = event.getPlayer();
        ItemStack item = seller.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Check for the HeadHunter player-head PDC tag.
        String ownerStr = meta.getPersistentDataContainer()
                .get(HEAD_OWNER_KEY, PersistentDataType.STRING);
        if (ownerStr == null) return;

        UUID victimUUID;
        try {
            victimUUID = UUID.fromString(ownerStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("BountyListener: malformed UUID in player head PDC: " + ownerStr);
            return;
        }

        long bountyAmount = bountyManager.getBounty(victimUUID);
        if (bountyAmount <= 0) {
            // No bounty — let HeadHunter's HeadSellListener handle it.
            return;
        }

        Economy economy = BountySystem.getEconomy();
        if (economy == null) {
            seller.sendMessage(msg("&cEconomy is unavailable — bounty cannot be paid out."));
            return;
        }

        // Claim the bounty.
        event.setCancelled(true);

        economy.depositPlayer(seller, (double) bountyAmount);
        bountyManager.removeBounty(victimUUID);

        // Remove exactly one head from the hand.
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            seller.getInventory().setItemInMainHand(null);
        }

        // Resolve victim name for messages.
        OfflinePlayer victim = Bukkit.getOfflinePlayer(victimUUID);
        String victimName = victim.getName() != null ? victim.getName() : victimUUID.toString();

        seller.sendMessage(msg("&aYou claimed a &e$" + bountyAmount + " &abounty on &e" + victimName + "&a!"));

        // Notify victim if they're online.
        Player onlineVictim = Bukkit.getPlayer(victimUUID);
        if (onlineVictim != null) {
            onlineVictim.sendMessage(msg(
                    "&cYour bounty of &e$" + bountyAmount
                    + " &cwas claimed by &e" + seller.getName() + "&c!"));
        }
    }

    private static Component msg(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
    }
}
