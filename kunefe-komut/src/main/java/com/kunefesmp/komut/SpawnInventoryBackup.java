package com.kunefesmp.komut;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpawnInventoryBackup {
    private final KunefeKomut plugin;
    private final ItemDataHelper helper;
    private final File dataFile;
    private FileConfiguration dataConfig;

    private final Map<UUID, PlayerBackupData> cache = new HashMap<>();

    public static class PlayerBackupData {
        private final ItemStack[] storage;
        private final ItemStack[] armor;
        private final ItemStack offHand;

        public PlayerBackupData(ItemStack[] storage, ItemStack[] armor, ItemStack offHand) {
            this.storage = storage;
            this.armor = armor;
            this.offHand = offHand;
        }

        public ItemStack[] getStorage() {
            return storage;
        }

        public ItemStack[] getArmor() {
            return armor;
        }

        public ItemStack getOffHand() {
            return offHand;
        }
    }

    public SpawnInventoryBackup(KunefeKomut plugin, ItemDataHelper helper) {
        this.plugin = plugin;
        this.helper = helper;
        this.dataFile = new File(plugin.getDataFolder(), "saved_inventories.yml");
        loadFromFile();
    }

    public synchronized void loadFromFile() {
        cache.clear();
        if (!dataFile.exists()) {
            return;
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = dataConfig.getConfigurationSection("inventories");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    List<?> rawStorage = dataConfig.getList("inventories." + key + ".storage");
                    List<?> rawArmor = dataConfig.getList("inventories." + key + ".armor");
                    ItemStack offHand = dataConfig.getItemStack("inventories." + key + ".offhand");

                    ItemStack[] storage = listToArray(rawStorage, 36);
                    ItemStack[] armor = listToArray(rawArmor, 4);

                    cache.put(uuid, new PlayerBackupData(storage, armor, offHand));
                } catch (Exception e) {
                    plugin.getLogger().warning("Envanter yüklenirken hata (" + key + "): " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info(cache.size() + " adet kaydedilmiş oyuncu envanteri dosyadan yüklendi.");
    }

    public synchronized void saveToFile() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }
        dataConfig.set("inventories", null);
        for (Map.Entry<UUID, PlayerBackupData> entry : cache.entrySet()) {
            String path = "inventories." + entry.getKey().toString();
            dataConfig.set(path + ".storage", entry.getValue().getStorage());
            dataConfig.set(path + ".armor", entry.getValue().getArmor());
            dataConfig.set(path + ".offhand", entry.getValue().getOffHand());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("saved_inventories.yml kaydedilemedi: " + e.getMessage());
        }
    }

    public boolean hasBackup(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public synchronized void backupAndClear(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();

        // Daha önce kaydedilmişse üzerine yazma! (Mevcut güvenli yedeği koru)
        if (cache.containsKey(uuid)) {
            return;
        }

        // 1. Storage içeriğini al ve spawn eşyalarını filtrele
        ItemStack[] rawStorage = player.getInventory().getStorageContents();
        ItemStack[] cleanStorage = new ItemStack[36];
        for (int i = 0; i < Math.min(rawStorage.length, 36); i++) {
            ItemStack it = rawStorage[i];
            if (it != null && !it.getType().isAir() && !helper.isSpawnItem(it)) {
                cleanStorage[i] = it.clone();
            }
        }

        // 2. Zırh içeriğini al
        ItemStack[] rawArmor = player.getInventory().getArmorContents();
        ItemStack[] cleanArmor = new ItemStack[4];
        for (int i = 0; i < Math.min(rawArmor.length, 4); i++) {
            ItemStack it = rawArmor[i];
            if (it != null && !it.getType().isAir() && !helper.isSpawnItem(it)) {
                cleanArmor[i] = it.clone();
            }
        }

        // 3. İkinci el (offhand) içeriğini al
        ItemStack rawOffHand = player.getInventory().getItemInOffHand();
        ItemStack cleanOffHand = (rawOffHand != null && !rawOffHand.getType().isAir() && !helper.isSpawnItem(rawOffHand))
                ? rawOffHand.clone() : null;

        PlayerBackupData backup = new PlayerBackupData(cleanStorage, cleanArmor, cleanOffHand);
        cache.put(uuid, backup);
        saveToFile();

        // 4. Oyuncunun envanterini, zırhını ve sol elini tamamen temizle
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();
    }

    public synchronized void restore(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();

        PlayerBackupData backup = cache.remove(uuid);
        if (backup != null) {
            saveToFile();

            ItemStack[] storage = backup.getStorage();
            ItemStack[] armor = backup.getArmor();
            ItemStack offHand = backup.getOffHand();

            player.getInventory().setStorageContents(storage);
            player.getInventory().setArmorContents(armor);
            player.getInventory().setItemInOffHand(offHand);
            player.updateInventory();
        }
    }

    private static ItemStack[] listToArray(List<?> list, int length) {
        ItemStack[] array = new ItemStack[length];
        if (list != null) {
            for (int i = 0; i < Math.min(list.size(), length); i++) {
                Object obj = list.get(i);
                if (obj instanceof ItemStack is) {
                    array[i] = is.clone();
                }
            }
        }
        return array;
    }
}
