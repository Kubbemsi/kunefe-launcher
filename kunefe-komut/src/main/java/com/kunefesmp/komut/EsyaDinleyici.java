package com.kunefesmp.komut;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!helper.hasCommand(item)) return;

        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);

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
            if (p.isOnline()) {
                if (spawnManager.isInSpawn(p.getLocation())) {
                    spawnManager.handlePlayerEnterSpawn(p);
                } else {
                    spawnManager.handlePlayerLeaveSpawn(p);
                }
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // 1. Tıklanan eşya veya imleçteki eşya spawn eşyası ise hareketi kesinlikle engelle
        if (helper.isSpawnItem(event.getCurrentItem()) || helper.isSpawnItem(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        // 2. Sayı tuşları (1-9) ile hotbar takası yapılıyorsa engelle
        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0 && hotbarButton < 9) {
                ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(hotbarButton);
                if (helper.isSpawnItem(hotbarItem) || helper.isSpawnItem(event.getCurrentItem())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // 3. İkinci ele takas (F tuşu) veya çift tıklama toplama
        if (event.getClick() == ClickType.SWAP_OFFHAND || event.getClick() == ClickType.DOUBLE_CLICK) {
            if (helper.isSpawnItem(event.getCurrentItem()) || helper.isSpawnItem(event.getCursor())) {
                event.setCancelled(true);
                return;
            }
        }

        // 4. Shift ile sandığa veya başka yuvaya taşıma
        if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (helper.isSpawnItem(event.getCurrentItem())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (helper.isSpawnItem(event.getOldCursor())) {
            event.setCancelled(true);
            return;
        }
        for (int slot : event.getRawSlots()) {
            ItemStack item = event.getView().getItem(slot);
            if (helper.isSpawnItem(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (helper.isSpawnItem(event.getMainHandItem()) || helper.isSpawnItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawnBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("kunefekomut.bypass")) return;
        if (!spawnManager.isProtectInteractions()) return;
        if (!spawnManager.isInSpawn(player.getLocation())) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;
            Material mat = block.getType();
            String name = mat.name();

            // Tabela düzenleme engeli
            if (name.contains("SIGN")) {
                event.setCancelled(true);
                event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
                return;
            }

            // Tuzak kapısı, kapı, çit kapısı, buton, şalter engeli
            if (name.endsWith("_TRAPDOOR")
                    || name.endsWith("_DOOR")
                    || name.endsWith("_FENCE_GATE")
                    || name.endsWith("_BUTTON")
                    || mat == Material.LEVER
                    || mat == Material.REPEATER
                    || mat == Material.COMPARATOR) {
                event.setCancelled(true);
                event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("kunefekomut.bypass")) return;
        if (!spawnManager.isProtectInteractions()) return;
        if (spawnManager.isInSpawn(event.getBlock().getLocation())) {
            event.setCancelled(true);
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
