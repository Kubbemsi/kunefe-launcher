package com.kunefesmp.komut;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class SpawnManager {
    private final KunefeKomut plugin;
    private final ItemDataHelper helper;

    private boolean enabled;
    private String spawnWorld;
    private boolean onlyCheckWorld;
    private double minX, maxX, minY, maxY, minZ, maxZ;
    private int preferredSlot;
    private ItemStack cachedSpawnItem;

    public SpawnManager(KunefeKomut plugin, ItemDataHelper helper) {
        this.plugin = plugin;
        this.helper = helper;
        loadFromConfig();
    }

    public void loadFromConfig() {
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("spawn-system.enabled", true);
        this.spawnWorld = config.getString("spawn-system.world", "spawn");
        this.onlyCheckWorld = config.getBoolean("spawn-system.only-check-world", false);

        this.minX = config.getDouble("spawn-system.region.min-x", -150.0);
        this.maxX = config.getDouble("spawn-system.region.max-x", 160.0);
        this.minY = config.getDouble("spawn-system.region.min-y", -10.0);
        this.maxY = config.getDouble("spawn-system.region.max-y", 120.0);
        this.minZ = config.getDouble("spawn-system.region.min-z", 400.0);
        this.maxZ = config.getDouble("spawn-system.region.max-z", 710.0);

        this.preferredSlot = config.getInt("spawn-system.slot", 4);

        if (config.contains("spawn-system.item")) {
            this.cachedSpawnItem = config.getItemStack("spawn-system.item");
        } else {
            this.cachedSpawnItem = null;
        }
    }

    public boolean isInSpawn(Location loc) {
        if (!enabled || loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(spawnWorld)) return false;
        if (onlyCheckWorld) return true;

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean hasSpawnItemConfigured() {
        return cachedSpawnItem != null;
    }

    public ItemStack getSpawnItem() {
        return cachedSpawnItem != null ? cachedSpawnItem.clone() : null;
    }

    public int getPreferredSlot() {
        return preferredSlot;
    }

    public void setSpawnItem(ItemStack item, int slot) {
        this.preferredSlot = Math.max(0, Math.min(8, slot));
        if (item != null) {
            ItemStack clone = item.clone();
            clone.setAmount(1);
            helper.markAsSpawnItem(clone, true);
            this.cachedSpawnItem = clone;
            plugin.getConfig().set("spawn-system.item", clone);
        } else {
            this.cachedSpawnItem = null;
            plugin.getConfig().set("spawn-system.item", null);
        }
        plugin.getConfig().set("spawn-system.slot", this.preferredSlot);
        plugin.saveConfig();

        // Çevrimiçi oyuncuları güncelle
        updateAllPlayers();
    }

    public void giveSpawnItem(Player player) {
        if (!enabled || cachedSpawnItem == null || player == null || !player.isOnline()) return;

        // Oyuncuda zaten spawn eşyası var mı kontrol et
        if (hasSpawnItem(player)) return;

        ItemStack item = cachedSpawnItem.clone();
        ItemStack currentSlotItem = player.getInventory().getItem(preferredSlot);

        if (currentSlotItem == null || currentSlotItem.getType().isAir()) {
            player.getInventory().setItem(preferredSlot, item);
        } else {
            // Yuva doluysa envanterdeki ilk boş yere koy veya o yuvayı kaydır
            player.getInventory().addItem(item);
        }
    }

    public void removeSpawnItem(Player player) {
        if (player == null || !player.isOnline()) return;

        boolean removed = false;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && helper.isSpawnItem(it)) {
                player.getInventory().setItem(i, null);
                removed = true;
            }
        }
        if (removed) {
            player.updateInventory();
        }
    }

    public boolean hasSpawnItem(Player player) {
        if (player == null) return false;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null && helper.isSpawnItem(it)) {
                return true;
            }
        }
        return false;
    }

    public void updateAllPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInSpawn(p.getLocation())) {
                giveSpawnItem(p);
            } else {
                removeSpawnItem(p);
            }
        }
    }
}
