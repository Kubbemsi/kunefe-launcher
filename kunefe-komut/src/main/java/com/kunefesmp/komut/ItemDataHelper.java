package com.kunefesmp.komut;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemDataHelper {
    private final KunefeKomut plugin;
    public final NamespacedKey keyCommand;
    public final NamespacedKey keyType;
    public final NamespacedKey keyConsume;
    public final NamespacedKey keyCooldown;

    public ItemDataHelper(KunefeKomut plugin) {
        this.plugin = plugin;
        this.keyCommand = new NamespacedKey(plugin, "command");
        this.keyType = new NamespacedKey(plugin, "type");
        this.keyConsume = new NamespacedKey(plugin, "consume");
        this.keyCooldown = new NamespacedKey(plugin, "cooldown");
    }

    public enum CommandType {
        PLAYER,
        CONSOLE
    }

    public boolean hasCommand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(keyCommand, PersistentDataType.STRING);
    }

    public String getCommand(ItemStack item) {
        if (!hasCommand(item)) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(keyCommand, PersistentDataType.STRING);
    }

    public CommandType getType(ItemStack item) {
        if (!hasCommand(item)) return CommandType.PLAYER;
        ItemMeta meta = item.getItemMeta();
        String typeStr = meta.getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
        if ("CONSOLE".equalsIgnoreCase(typeStr)) {
            return CommandType.CONSOLE;
        }
        return CommandType.PLAYER;
    }

    public boolean isConsume(ItemStack item) {
        if (!hasCommand(item)) return false;
        ItemMeta meta = item.getItemMeta();
        Byte val = meta.getPersistentDataContainer().get(keyConsume, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    public int getCooldown(ItemStack item) {
        if (!hasCommand(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        Integer val = meta.getPersistentDataContainer().get(keyCooldown, PersistentDataType.INTEGER);
        return val != null ? val : 0;
    }

    public void bindCommand(ItemStack item, String cmd, CommandType type) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // "/" işaretini temizle
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyCommand, PersistentDataType.STRING, cmd);
        pdc.set(keyType, PersistentDataType.STRING, type.name());

        // Varsayılan consume yoksa 0 ata
        if (!pdc.has(keyConsume, PersistentDataType.BYTE)) {
            pdc.set(keyConsume, PersistentDataType.BYTE, (byte) 0);
        }

        item.setItemMeta(meta);
        refreshVisualLore(item);
    }

    public void setConsume(ItemStack item, boolean consume) {
        if (!hasCommand(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keyConsume, PersistentDataType.BYTE, consume ? (byte) 1 : (byte) 0);
        item.setItemMeta(meta);
        refreshVisualLore(item);
    }

    public void setCooldown(ItemStack item, int seconds) {
        if (!hasCommand(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keyCooldown, PersistentDataType.INTEGER, Math.max(0, seconds));
        item.setItemMeta(meta);
        refreshVisualLore(item);
    }

    public void clearCommand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(keyCommand);
        pdc.remove(keyType);
        pdc.remove(keyConsume);
        pdc.remove(keyCooldown);

        item.setItemMeta(meta);
        removePluginLore(item);
    }

    public void setDisplayName(ItemStack item, String name) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.displayName(colorizeComponent(name));
        item.setItemMeta(meta);
    }

    public void addCustomLore(ItemStack item, String line) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();

        // Eklenti lore'larının hemen öncesine ekle
        int insertIndex = lore.size();
        for (int i = 0; i < lore.size(); i++) {
            String str = LegacyComponentSerializer.legacySection().serialize(lore.get(i));
            if (isPluginLoreLine(str)) {
                insertIndex = i;
                break;
            }
        }

        lore.add(insertIndex, colorizeComponent(line));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    public void clearAllLore(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.lore(null);
        item.setItemMeta(meta);
        if (hasCommand(item)) {
            refreshVisualLore(item);
        }
    }

    public void refreshVisualLore(ItemStack item) {
        if (item == null || !hasCommand(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();

        // Eski eklenti satırlarını filtrele
        List<Component> cleanLore = new ArrayList<>();
        for (Component c : lore) {
            String str = LegacyComponentSerializer.legacySection().serialize(c);
            if (!isPluginLoreLine(str)) {
                cleanLore.add(c);
            }
        }

        String cmd = getCommand(item);
        CommandType type = getType(item);
        boolean consume = isConsume(item);
        int cd = getCooldown(item);

        // Yeni satırları oluştur
        String cmdLine;
        if (type == CommandType.CONSOLE) {
            cmdLine = plugin.getConfig().getString("lore-format.console", "&7► Sağ tıklandığında: &6/{cmd} &8(Özel)")
                    .replace("{cmd}", cmd);
        } else {
            cmdLine = plugin.getConfig().getString("lore-format.player", "&7► Sağ tıklandığında: &e/{cmd}")
                    .replace("{cmd}", cmd);
        }
        cleanLore.add(colorizeComponent(cmdLine));

        if (cd > 0) {
            String cdLine = plugin.getConfig().getString("lore-format.cooldown", "&8⏱ Bekleme Süresi: &7{seconds} sn")
                    .replace("{seconds}", String.valueOf(cd));
            cleanLore.add(colorizeComponent(cdLine));
        }

        if (consume) {
            String singleLine = plugin.getConfig().getString("lore-format.single-use", "&c⚠ Tek kullanımlıktır (Kullanılınca kaybolur)");
            cleanLore.add(colorizeComponent(singleLine));
        }

        meta.lore(cleanLore);
        item.setItemMeta(meta);
    }

    private void removePluginLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) return;

        List<Component> cleanLore = new ArrayList<>();
        for (Component c : meta.lore()) {
            String str = LegacyComponentSerializer.legacySection().serialize(c);
            if (!isPluginLoreLine(str)) {
                cleanLore.add(c);
            }
        }
        meta.lore(cleanLore.isEmpty() ? null : cleanLore);
        item.setItemMeta(meta);
    }

    private boolean isPluginLoreLine(String line) {
        if (line == null) return false;
        return line.contains("Sağ tıklandığında:")
                || line.contains("Tek kullanımlıktır")
                || line.contains("Bekleme Süresi:");
    }

    public static Component colorizeComponent(String msg) {
        if (msg == null) return Component.empty();
        String legacy = ChatColor.translateAlternateColorCodes('&', msg);
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }
}
