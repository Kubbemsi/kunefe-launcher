package com.kunefesmp.komut;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class KunefeKomut extends JavaPlugin {
    private ItemDataHelper itemHelper;
    private SpawnManager spawnManager;
    private String prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        this.itemHelper = new ItemDataHelper(this);
        this.spawnManager = new SpawnManager(this, itemHelper);

        KomutYonetici komutYonetici = new KomutYonetici(this, itemHelper, spawnManager);
        if (getCommand("kunefekomut") != null) {
            getCommand("kunefekomut").setExecutor(komutYonetici);
            getCommand("kunefekomut").setTabCompleter(komutYonetici);
        }

        getServer().getPluginManager().registerEvents(new EsyaDinleyici(this, itemHelper, spawnManager), this);

        // Çevrimiçi olan oyuncular varsa spawn kontrolü yap
        spawnManager.updateAllPlayers();

        getLogger().info("=========================================");
        getLogger().info(" KunefeKomut v1.1.0 basariyla yuklendi! ");
        getLogger().info(" Eşyaya sağ tıklama & Spawn eşyası sistemi aktif. ");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("KunefeKomut devre disi birakildi.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        loadSettings();
        if (spawnManager != null) {
            spawnManager.loadFromConfig();
        }
    }

    private void loadSettings() {
        this.prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", "&6&l[KünefeKomut]&r "));
    }

    public String getPrefix() {
        return prefix;
    }

    public ItemDataHelper getItemHelper() {
        return itemHelper;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }
}
