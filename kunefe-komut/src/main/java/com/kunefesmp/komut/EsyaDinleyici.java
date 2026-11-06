package com.kunefesmp.komut;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EsyaDinleyici implements Listener {
    private final KunefeKomut plugin;
    private final ItemDataHelper helper;
    private final SpawnManager spawnManager;
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();

    public EsyaDinleyici(KunefeKomut plugin, ItemDataHelper helper, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.helper = helper;
        this.spawnManager = spawnManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!helper.hasCommand(item)) return;

        event.setCancelled(true);

        if (!player.hasPermission("kunefekomut.use")) {
            player.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.no-permission", "&cYetkin yok!")));
            return;
        }

        // Sadece Spawn bölgesinde çalışma kontrolü
        if (helper.isSpawnOnly(item) || helper.isSpawnItem(item)) {
            if (!spawnManager.isInSpawn(player.getLocation())) {
                String errorMsg = plugin.getConfig().getString("messages.spawn-only-error", "&c[!] Bu eşyayı yalnızca spawn bölgesinde kullanabilirsin!");
                player.sendMessage(plugin.getPrefix() + colorize(errorMsg));
                return;
            }
        }

        // Cooldown kontrolü
        int itemCd = helper.getCooldown(item);
        int defaultCd = plugin.getConfig().getInt("cooldown-default-seconds", 1);
        int effectiveCd = Math.max(itemCd, defaultCd);

        long now = System.currentTimeMillis();
        long lastUse = playerCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cdMillis = effectiveCd * 1000L;

        if (now - lastUse < cdMillis) {
            long remainingSec = ((lastUse + cdMillis) - now) / 1000L + 1;
            String waitMsg = plugin.getConfig().getString("messages.cooldown-wait", "&cLütfen {seconds} saniye bekle!")
                    .replace("{seconds}", String.valueOf(remainingSec));
            player.sendMessage(plugin.getPrefix() + colorize(waitMsg));
            return;
        }
        playerCooldowns.put(player.getUniqueId(), now);

        String cmd = helper.getCommand(item);
        ItemDataHelper.CommandType type = helper.getType(item);
        boolean isConsume = helper.isConsume(item);

        String finalCmd = cmd
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%world%", player.getWorld().getName());

        if (isConsume) {
            int amount = item.getAmount();
            if (amount > 1) {
                item.setAmount(amount - 1);
                player.getInventory().setItemInMainHand(item);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }

        if (type == ItemDataHelper.CommandType.CONSOLE) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        } else {
            player.performCommand(finalCmd);
        }

        if (plugin.getConfig().getBoolean("play-sound", true)) {
            try {
                String soundName = plugin.getConfig().getString("sound-name", "UI_BUTTON_CLICK");
                float volume = (float) plugin.getConfig().getDouble("sound-volume", 1.0);
                float pitch = (float) plugin.getConfig().getDouble("sound-pitch", 1.2);
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception ignored) {
            }
        }
    }

    // ==========================================
    //       OTOMATİK SPAWN EŞYASI YÖNETİMİ
    // ==========================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && spawnManager.isInSpawn(p.getLocation())) {
                spawnManager.giveSpawnItem(p);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        if (spawnManager.isInSpawn(p.getLocation())) {
            spawnManager.giveSpawnItem(p);
        } else {
            spawnManager.removeSpawnItem(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player p = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        boolean wasIn = spawnManager.isInSpawn(from);
        boolean nowIn = spawnManager.isInSpawn(to);

        if (wasIn && !nowIn) {
            spawnManager.removeSpawnItem(p);
        } else if (!wasIn && nowIn) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline() && spawnManager.isInSpawn(p.getLocation())) {
                    spawnManager.giveSpawnItem(p);
                }
            }, 2L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ() && from.getBlockY() == to.getBlockY()) return;

        boolean wasIn = spawnManager.isInSpawn(from);
        boolean nowIn = spawnManager.isInSpawn(to);

        if (wasIn && !nowIn) {
            spawnManager.removeSpawnItem(event.getPlayer());
        } else if (!wasIn && nowIn) {
            spawnManager.giveSpawnItem(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (helper.isSpawnItem(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getPrefix() + colorize("&c[!] Bu spawn eşyasını yere atamazsın!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        // Oyuncu kendi envanteri dışında bir depolama birimine (sandık vb.) tıklıyorsa engelle
        if (!event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
            ItemStack current = event.getCurrentItem();
            ItemStack cursor = event.getCursor();
            if (helper.isSpawnItem(current) || helper.isSpawnItem(cursor)) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(plugin.getPrefix() + colorize("&c[!] Bu spawn eşyasını sandıklara koyamazsın!"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(helper::isSpawnItem);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && spawnManager.isInSpawn(p.getLocation())) {
                spawnManager.giveSpawnItem(p);
            }
        }, 5L);
    }

    private String colorize(String msg) {
        if (msg == null) return "";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
