package com.kunefesmp.komut;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class KunefeKomut extends JavaPlugin {
    private ItemDataHelper itemHelper;
    private String prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        this.itemHelper = new ItemDataHelper(this);

        KomutYonetici komutYonetici = new KomutYonetici(this, itemHelper);
        if (getCommand("kunefekomut") != null) {
            getCommand("kunefekomut").setExecutor(komutYonetici);
            getCommand("kunefekomut").setTabCompleter(komutYonetici);
        }

        getServer().getPluginManager().registerEvents(new EsyaDinleyici(this, itemHelper), this);

        getLogger().info("=========================================");
        getLogger().info(" KunefeKomut v1.0.0 basariyla yuklendi! ");
        getLogger().info(" Eşyaya sağ tıklama komut sistemi aktif. ");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("KunefeKomut devre disi birakildi.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        loadSettings();
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
}
