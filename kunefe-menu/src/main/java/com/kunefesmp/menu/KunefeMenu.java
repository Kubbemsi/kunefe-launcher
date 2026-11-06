package com.kunefesmp.menu;

import com.kunefesmp.menu.command.MenuCommand;
import com.kunefesmp.menu.gui.KunefeMenuHolder;
import com.kunefesmp.menu.gui.MenuListener;
import com.kunefesmp.menu.gui.MenuManager;
import com.kunefesmp.menu.hook.Hooks;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class KunefeMenu extends JavaPlugin {

    private static KunefeMenu instance;
    private Hooks hooks;
    private MenuManager menuManager;

    public static KunefeMenu getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Varsayılan config'i kaydet
        saveDefaultConfig();

        // Entegrasyonları başlat
        this.hooks = new Hooks(this);

        // Menü yöneticisini başlat ve menüleri yükle
        this.menuManager = new MenuManager(this);
        this.menuManager.loadMenus(getConfig());

        // Dinleyicileri kaydet
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        // Komutları kaydet
        MenuCommand menuCommand = new MenuCommand(this);

        PluginCommand menuCmd = getCommand("menu");
        if (menuCmd != null) {
            menuCmd.setExecutor(menuCommand);
            menuCmd.setTabCompleter(menuCommand);
        }

        PluginCommand kurallarCmd = getCommand("kurallar");
        if (kurallarCmd != null) {
            kurallarCmd.setExecutor(menuCommand);
            kurallarCmd.setTabCompleter(menuCommand);
        }

        PluginCommand adminCmd = getCommand("kunefemenu");
        if (adminCmd != null) {
            adminCmd.setExecutor(menuCommand);
            adminCmd.setTabCompleter(menuCommand);
        }

        getLogger().info("=========================================");
        getLogger().info(" KunefeMenu v" + getDescription().getVersion() + " basariyla yuklendi!");
        getLogger().info(" Kapsamli Sunucu Menusu (/menu) aktif!");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        // Açık olan tüm menüleri güvenle kapat
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                if (p.getOpenInventory().getTopInventory().getHolder() instanceof KunefeMenuHolder) {
                    p.closeInventory();
                }
            } catch (Throwable ignored) {}
        }

        getLogger().info("KunefeMenu devre disi birakildi.");
    }

    public void reloadPlugin() {
        reloadConfig();
        if (menuManager != null) {
            menuManager.loadMenus(getConfig());
        }
    }

    public Hooks getHooks() {
        return hooks;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }
}
