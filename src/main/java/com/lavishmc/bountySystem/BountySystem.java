package com.lavishmc.bountySystem;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class BountySystem extends JavaPlugin {

    private static Economy economy;
    private BountyManager bountyManager;

    public static Economy getEconomy() {
        return economy;
    }

    @Override
    public void onEnable() {
        // Resolve Vault economy (soft dependency — null if unavailable).
        economy = null;
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp =
                    getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }
        if (economy == null) {
            getLogger().warning("Vault not found or no economy plugin loaded — bounty placing/claiming disabled.");
        }

        bountyManager = new BountyManager(this);
        bountyManager.load();

        getServer().getPluginManager().registerEvents(new BountyListener(this, bountyManager), this);

        BountyCommand bountyCommand = new BountyCommand(bountyManager);
        Objects.requireNonNull(getCommand("bounty")).setExecutor(bountyCommand);
        Objects.requireNonNull(getCommand("bounty")).setTabCompleter(bountyCommand);

        getLogger().info("BountySystem enabled.");
    }

    @Override
    public void onDisable() {
        if (bountyManager != null) {
            bountyManager.save();
        }
        getLogger().info("BountySystem disabled.");
    }
}
