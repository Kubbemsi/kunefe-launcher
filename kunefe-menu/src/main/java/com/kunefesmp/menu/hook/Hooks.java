package com.kunefesmp.menu.hook;

import com.kunefesmp.menu.KunefeMenu;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.UUID;

public class Hooks {

    private final KunefeMenu plugin;
    private Economy vaultEconomy = null;
    private boolean papiPresent = false;
    private boolean playerPointsPresent = false;

    public Hooks(KunefeMenu plugin) {
        this.plugin = plugin;
        setupVault();
        setupPlaceholderAPI();
        setupPlayerPoints();
    }

    private void setupVault() {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            try {
                RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    vaultEconomy = rsp.getProvider();
                    plugin.getLogger().info("Vault Economy entegrasyonu sağlandı.");
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Vault bağlanırken hata: " + t.getMessage());
            }
        }
    }

    private void setupPlaceholderAPI() {
        papiPresent = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (papiPresent) {
            plugin.getLogger().info("PlaceholderAPI entegrasyonu sağlandı.");
        }
    }

    private void setupPlayerPoints() {
        playerPointsPresent = Bukkit.getPluginManager().isPluginEnabled("PlayerPoints");
        if (playerPointsPresent) {
            plugin.getLogger().info("PlayerPoints entegrasyonu sağlandı.");
        }
    }

    public String getBalance(Player player) {
        if (vaultEconomy != null) {
            try {
                double bal = vaultEconomy.getBalance(player);
                return String.format("%,.0f", bal);
            } catch (Throwable ignored) {}
        }
        return "0";
    }

    public int getPoints(Player player) {
        if (playerPointsPresent) {
            try {
                Class<?> ppClass = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
                Method getInstance = ppClass.getMethod("getInstance");
                Object instance = getInstance.invoke(null);
                Method getAPI = ppClass.getMethod("getAPI");
                Object api = getAPI.invoke(instance);
                Method look = api.getClass().getMethod("look", UUID.class);
                Object result = look.invoke(api, player.getUniqueId());
                if (result instanceof Number num) {
                    return num.intValue();
                }
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    public String getRank(Player player) {
        if (papiPresent) {
            try {
                String rank = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
                if (rank != null && !rank.isEmpty() && !rank.equals("%luckperms_prefix%")) {
                    return rank.trim();
                }
            } catch (Throwable ignored) {}
        }
        return player.isOp() ? "&cYetkili" : "&7Oyuncu";
    }

    public String parse(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Temel dahili yer tutucular
        String result = text
                .replace("%player%", player.getName())
                .replace("%player_name%", player.getName())
                .replace("%balance%", getBalance(player))
                .replace("%points%", String.valueOf(getPoints(player)))
                .replace("%rank%", getRank(player))
                .replace("%ping%", String.valueOf(player.getPing()))
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%world%", player.getWorld().getName());

        // PlaceholderAPI yüklüyse diğer tüm PAPI etiketlerini çöz
        if (papiPresent) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result);
            } catch (Throwable ignored) {}
        }

        return result;
    }
}
