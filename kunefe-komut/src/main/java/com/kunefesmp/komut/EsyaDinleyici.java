package com.kunefesmp.komut;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EsyaDinleyici implements Listener {
    private final KunefeKomut plugin;
    private final ItemDataHelper helper;
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();

    public EsyaDinleyici(KunefeKomut plugin, ItemDataHelper helper) {
        this.plugin = plugin;
        this.helper = helper;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Sadece ana el sağ tıklamasını dinle (çift tetiklenmeyi önler)
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!helper.hasCommand(item)) return;

        // Blok yerleştirmeyi veya varsayılan blok etkileşimini iptal et
        event.setCancelled(true);

        if (!player.hasPermission("kunefekomut.use")) {
            player.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.no-permission", "&cYetkin yok!")));
            return;
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

        // Değişkenleri doldur
        String finalCmd = cmd
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%world%", player.getWorld().getName());

        // Tek kullanımlık ise eşyayı azalt
        if (isConsume) {
            int amount = item.getAmount();
            if (amount > 1) {
                item.setAmount(amount - 1);
                player.getInventory().setItemInMainHand(item);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }

        // Komutu çalıştır
        if (type == ItemDataHelper.CommandType.CONSOLE) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        } else {
            player.performCommand(finalCmd);
        }

        // Ses efekti
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

    private String colorize(String msg) {
        if (msg == null) return "";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
