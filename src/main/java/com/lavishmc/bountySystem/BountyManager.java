package com.lavishmc.bountySystem;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyManager {

    private final BountySystem plugin;
    private final HashMap<UUID, Long> bounties = new HashMap<>();
    private final File dataFile;

    public BountyManager(BountySystem plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "bounties.yml");
    }

    /** Loads bounties from bounties.yml. Called once on plugin enable. */
    public void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long amount = config.getLong(key);
                if (amount > 0) bounties.put(uuid, amount);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Skipping invalid UUID in bounties.yml: " + key);
            }
        }
        plugin.getLogger().info("Loaded " + bounties.size() + " bounty entries.");
    }

    /** Persists the current bounty map to bounties.yml. */
    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : bounties.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save bounties.yml: " + e.getMessage());
        }
    }

    /**
     * Adds {@code amount} to the existing bounty on {@code target}, or creates
     * a new one.  Persists immediately.
     */
    public void placeBounty(UUID target, long amount) {
        bounties.merge(target, amount, Long::sum);
        save();
    }

    /** Returns the current bounty on {@code target}, or 0 if none exists. */
    public long getBounty(UUID target) {
        return bounties.getOrDefault(target, 0L);
    }

    /** Removes all bounty on {@code target} and persists. */
    public void removeBounty(UUID target) {
        bounties.remove(target);
        save();
    }

    /**
     * Returns all bounty entries sorted highest to lowest by amount.
     */
    public List<Map.Entry<UUID, Long>> getAllBounties() {
        List<Map.Entry<UUID, Long>> entries = new ArrayList<>(bounties.entrySet());
        entries.sort(Comparator.comparingLong(Map.Entry<UUID, Long>::getValue).reversed());
        return entries;
    }
}
