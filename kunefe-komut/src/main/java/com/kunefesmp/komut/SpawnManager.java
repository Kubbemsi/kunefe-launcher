package com.kunefesmp.komut;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SpawnManager {
    private final KunefeKomut plugin;
    private final ItemDataHelper helper;

    private boolean enabled;
    private String spawnWorld;
    private boolean onlyCheckWorld;
    private double minX, maxX, minY, maxY, minZ, maxZ;
    private int preferredSlot;
    private ItemStack cachedSpawnItem;
    private boolean adventureMode;
    private boolean protectInteractions;
    private boolean separateInventory;

    private final SpawnInventoryBackup inventoryBackup;
    private final Map<Integer, ItemStack> spawnItems = new HashMap<>();

    public SpawnManager(KunefeKomut plugin, ItemDataHelper helper) {
        this.plugin = plugin;
        this.helper = helper;
        this.inventoryBackup = new SpawnInventoryBackup(plugin, helper);
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
        this.adventureMode = config.getBoolean("spawn-system.adventure-mode", true);
        this.protectInteractions = config.getBoolean("spawn-system.protect-interactions", true);
        this.separateInventory = config.getBoolean("spawn-system.separate-inventory", true);
        this.spawnItems.clear();

        // 1. Config'deki 'spawn-system.items' listesini yükle
        ConfigurationSection itemsSec = config.getConfigurationSection("spawn-system.items");
        if (itemsSec != null) {
            for (String key : itemsSec.getKeys(false)) {
                ConfigurationSection sec = itemsSec.getConfigurationSection(key);
                if (sec == null) continue;

                int slot = sec.getInt("slot", 0);
                if (slot < 0 || slot > 8) continue;

                String matStr = sec.getString("material", "COMPASS").toUpperCase(Locale.ROOT);
                Material mat = Material.matchMaterial(matStr);
                if (mat == null) mat = Material.COMPASS;

                ItemStack stack = new ItemStack(mat, 1);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    String name = sec.getString("name", "");
                    if (!name.isEmpty()) {
                        meta.setDisplayName(colorize(name));
                    }
                    List<String> lore = sec.getStringList("lore");
                    if (!lore.isEmpty()) {
                        List<String> coloredLore = new ArrayList<>();
                        for (String l : lore) {
                            coloredLore.add(colorize(l));
                        }
                        meta.setLore(coloredLore);
                    }
                    if (sec.getBoolean("glow", false)) {
                        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                    stack.setItemMeta(meta);
                }

                String cmd = sec.getString("command", "");
                boolean isConsole = sec.getBoolean("is-console", false);
                int cooldown = sec.getInt("cooldown", 1);

                if (!cmd.isEmpty()) {
                    helper.bindCommand(stack, cmd, isConsole ? ItemDataHelper.CommandType.CONSOLE : ItemDataHelper.CommandType.PLAYER);
                }
                helper.setCooldown(stack, cooldown);
                helper.markAsSpawnItem(stack, true);

                this.spawnItems.put(slot, stack);
            }
        }

        // 2. Geriye dönük uyumluluk: 'spawn-system.item' varsa ekle
        if (config.contains("spawn-system.item")) {
            ItemStack singleItem = config.getItemStack("spawn-system.item");
            if (singleItem != null) {
                this.cachedSpawnItem = singleItem;
                if (!this.spawnItems.containsKey(this.preferredSlot)) {
                    this.spawnItems.put(this.preferredSlot, singleItem);
                }
            }
        } else if (!this.spawnItems.isEmpty()) {
            this.cachedSpawnItem = this.spawnItems.values().iterator().next();
        } else {
            this.cachedSpawnItem = null;
        }

        plugin.getLogger().info("Spawn eşyaları yüklendi: " + this.spawnItems.size() + " adet eşya aktif.");
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
        return !spawnItems.isEmpty() || cachedSpawnItem != null;
    }

    public ItemStack getSpawnItem() {
        return cachedSpawnItem != null ? cachedSpawnItem.clone() : (spawnItems.isEmpty() ? null : spawnItems.values().iterator().next().clone());
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
            this.spawnItems.put(this.preferredSlot, clone);
            plugin.getConfig().set("spawn-system.item", clone);
        } else {
            this.cachedSpawnItem = null;
            this.spawnItems.remove(this.preferredSlot);
            plugin.getConfig().set("spawn-system.item", null);
        }
        plugin.getConfig().set("spawn-system.slot", this.preferredSlot);
        plugin.saveConfig();

        // Çevrimiçi oyuncuları güncelle
        updateAllPlayers();
    }

    public void handlePlayerEnterSpawn(Player player) {
        if (!enabled || player == null || !player.isOnline()) return;

        // 1. Hayatta kalma envanterini (zırh, sol el ve eşyalar) güvenle yedekle ve temizle
        if (separateInventory) {
            inventoryBackup.backupAndClear(player);
        }

        // 2. Spawna girince macera moduna geçir (OP/Yetkililer hariç)
        if (adventureMode && !player.isOp() && !player.hasPermission("kunefekomut.bypass")) {
            if (player.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            }
        }

        // 3. Spawn eşyalarını hotbara dağıt
        giveSpawnItems(player);
    }

    public void handlePlayerLeaveSpawn(Player player) {
        if (player == null || !player.isOnline()) return;

        // 1. Spawn eşyalarını çantadan temizle
        removeSpawnItems(player);

        // 2. Spawndan çıkınca hayatta kalma (Survival) moduna geri döndür
        if (adventureMode && !player.isOp() && !player.hasPermission("kunefekomut.bypass")) {
            if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            }
        }

        // 3. Yedeklenmiş hayatta kalma envanterini, zırhları ve sol eli geri yükle
        if (separateInventory) {
            inventoryBackup.restore(player);
        }
    }

    public void giveSpawnItem(Player player) {
        handlePlayerEnterSpawn(player);
    }

    public void giveSpawnItems(Player player) {
        if (!enabled || player == null || !player.isOnline()) return;
        if (spawnItems.isEmpty()) return;

        for (Map.Entry<Integer, ItemStack> entry : spawnItems.entrySet()) {
            int slot = entry.getKey();
            ItemStack spawnItem = entry.getValue().clone();

            ItemStack current = player.getInventory().getItem(slot);

            // Oyuncuda zaten spawn eşyası varsa güncelle
            if (current != null && helper.isSpawnItem(current)) {
                player.getInventory().setItem(slot, spawnItem);
                continue;
            }

            // Ayrı envanter modu kapalıysa ve yuva doluysa boş alana taşı
            if (!separateInventory && current != null && !current.getType().isAir()) {
                int emptySlot = findEmptyStorageSlot(player);
                if (emptySlot != -1) {
                    player.getInventory().setItem(emptySlot, current);
                } else {
                    HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(current);
                    if (!leftovers.isEmpty()) {
                        continue;
                    }
                }
            }

            player.getInventory().setItem(slot, spawnItem);
        }
        player.updateInventory();
    }

    private int findEmptyStorageSlot(Player player) {
        for (int i = 9; i < 36; i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it == null || it.getType().isAir()) {
                return i;
            }
        }
        return -1;
    }

    public void removeSpawnItem(Player player) {
        handlePlayerLeaveSpawn(player);
    }

    public void removeSpawnItems(Player player) {
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

    public boolean isAdventureMode() {
        return adventureMode;
    }

    public boolean isProtectInteractions() {
        return protectInteractions;
    }

    public boolean isSeparateInventory() {
        return separateInventory;
    }

    public SpawnInventoryBackup getInventoryBackup() {
        return inventoryBackup;
    }

    public void updateAllPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInSpawn(p.getLocation())) {
                handlePlayerEnterSpawn(p);
            } else {
                handlePlayerLeaveSpawn(p);
            }
        }
    }

    private String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
