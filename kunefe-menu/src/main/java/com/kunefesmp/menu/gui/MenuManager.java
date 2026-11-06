package com.kunefesmp.menu.gui;

import com.kunefesmp.menu.KunefeMenu;
import com.kunefesmp.menu.util.ColorUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class MenuManager {

    private final KunefeMenu plugin;
    private final Map<String, MenuConfig> menus = new HashMap<>();

    public MenuManager(KunefeMenu plugin) {
        this.plugin = plugin;
    }

    public void loadMenus(FileConfiguration config) {
        menus.clear();
        ConfigurationSection menusSec = config.getConfigurationSection("menus");
        if (menusSec == null) {
            plugin.getLogger().warning("config.yml içinde 'menus' bölümü bulunamadı!");
            return;
        }

        for (String menuKey : menusSec.getKeys(false)) {
            ConfigurationSection sec = menusSec.getConfigurationSection(menuKey);
            if (sec == null) continue;

            String title = sec.getString("title", "&8Sunucu Menüsü");
            int size = sec.getInt("size", 54);
            if (sec.contains("rows")) {
                size = sec.getInt("rows") * 9;
            }
            String openSound = sec.getString("open-sound", null);

            MenuConfig menuConfig = new MenuConfig(menuKey, title, size, openSound);

            ConfigurationSection itemsSec = sec.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String itemKey : itemsSec.getKeys(false)) {
                    ConfigurationSection itemSec = itemsSec.getConfigurationSection(itemKey);
                    if (itemSec == null) continue;

                    List<Integer> slots = new ArrayList<>();
                    if (itemSec.isList("slots")) {
                        slots.addAll(itemSec.getIntegerList("slots"));
                    } else if (itemSec.contains("slot")) {
                        slots.add(itemSec.getInt("slot"));
                    }

                    String matName = itemSec.getString("material", "STONE").toUpperCase();
                    Material material = Material.matchMaterial(matName);
                    if (material == null) {
                        material = Material.STONE;
                    }

                    String name = itemSec.getString("name", itemSec.getString("display-name", ""));
                    List<String> lore = itemSec.getStringList("lore");
                    int amount = itemSec.getInt("amount", 1);
                    boolean glow = itemSec.getBoolean("glow", false);
                    String skullOwner = itemSec.getString("skull-owner", null);
                    int customModelData = itemSec.getInt("custom-model-data", 0);

                    List<String> actions = new ArrayList<>();
                    if (itemSec.isList("actions")) {
                        actions.addAll(itemSec.getStringList("actions"));
                    } else if (itemSec.isList("commands")) {
                        actions.addAll(itemSec.getStringList("commands"));
                    } else if (itemSec.contains("action")) {
                        actions.add(itemSec.getString("action"));
                    } else if (itemSec.contains("command")) {
                        actions.add(itemSec.getString("command"));
                    }

                    MenuItemConfig itemConfig = new MenuItemConfig(
                            itemKey, slots, material, name, lore, amount, glow, skullOwner, customModelData, actions
                    );
                    menuConfig.addItem(itemConfig);
                }
            }

            menus.put(menuKey.toLowerCase(), menuConfig);
            plugin.getLogger().info("Menü yüklendi: " + menuKey + " (" + menuConfig.getSize() + " slot)");
        }
    }

    public boolean openMenu(Player player, String menuKey) {
        if (menuKey == null || menuKey.isEmpty()) {
            menuKey = "main";
        }

        MenuConfig menuConfig = menus.get(menuKey.toLowerCase());
        if (menuConfig == null) {
            player.sendMessage(ColorUtils.colorize("&cMenü bulunamadı: " + menuKey));
            return false;
        }

        String rawTitle = menuConfig.getTitle();
        String parsedTitle = plugin.getHooks().parse(player, rawTitle);
        String finalTitle = ColorUtils.colorize(parsedTitle);

        KunefeMenuHolder holder = new KunefeMenuHolder(menuKey.toLowerCase());
        org.bukkit.inventory.Inventory inventory = Bukkit.createInventory(holder, menuConfig.getSize(), finalTitle);
        holder.setInventory(inventory);

        for (MenuItemConfig itemConfig : menuConfig.getItems()) {
            ItemStack stack = buildItemStack(player, itemConfig);
            for (int slot : itemConfig.getSlots()) {
                if (slot >= 0 && slot < menuConfig.getSize()) {
                    inventory.setItem(slot, stack);
                }
            }
        }

        player.openInventory(inventory);

        // Açılış sesi
        if (menuConfig.getOpenSound() != null && !menuConfig.getOpenSound().isEmpty()) {
            playSound(player, menuConfig.getOpenSound());
        }

        return true;
    }

    private ItemStack buildItemStack(Player player, MenuItemConfig config) {
        ItemStack item = new ItemStack(config.getMaterial(), config.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // İsim
        if (!config.getName().isEmpty()) {
            String parsedName = plugin.getHooks().parse(player, config.getName());
            meta.setDisplayName(ColorUtils.colorize(parsedName));
        }

        // Açıklama (Lore)
        if (!config.getLore().isEmpty()) {
            List<String> formattedLore = new ArrayList<>();
            for (String line : config.getLore()) {
                String parsedLine = plugin.getHooks().parse(player, line);
                formattedLore.add(ColorUtils.colorize(parsedLine));
            }
            meta.setLore(formattedLore);
        }

        // Kafa kontrolü (Player Head)
        if (meta instanceof SkullMeta skullMeta && config.getSkullOwner() != null) {
            String owner = config.getSkullOwner();
            if (owner.equalsIgnoreCase("%player%") || owner.equalsIgnoreCase("%player_name%")) {
                skullMeta.setOwningPlayer(player);
            } else {
                skullMeta.setOwner(owner);
            }
        }

        // Parlama (Glow)
        if (config.isGlow()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        );

        if (config.getCustomModelData() > 0) {
            meta.setCustomModelData(config.getCustomModelData());
        }

        item.setItemMeta(meta);
        return item;
    }

    public void executeActions(Player player, MenuItemConfig itemConfig) {
        if (itemConfig == null || itemConfig.getActions().isEmpty()) {
            return;
        }

        boolean hasExplicitSound = false;
        for (String action : itemConfig.getActions()) {
            if (action.startsWith("[sound]")) {
                hasExplicitSound = true;
                break;
            }
        }

        // Varsayılan tıklama sesi (eğer aksiyonlarda özel [sound] yoksa)
        if (!hasExplicitSound) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        }

        for (String rawAction : itemConfig.getActions()) {
            String action = plugin.getHooks().parse(player, rawAction).trim();

            if (action.equalsIgnoreCase("[close]")) {
                player.closeInventory();
            } else if (action.startsWith("[player]")) {
                String cmd = action.substring(8).trim();
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                final String finalCmd = cmd;
                Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(finalCmd));
            } else if (action.startsWith("[console]")) {
                String cmd = action.substring(9).trim();
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                final String finalCmd = cmd;
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
            } else if (action.startsWith("[message]")) {
                String msg = action.substring(9).trim();
                player.sendMessage(ColorUtils.colorize(msg));
            } else if (action.startsWith("[sound]")) {
                String soundStr = action.substring(7).trim();
                playSound(player, soundStr);
            } else if (action.startsWith("[menu]")) {
                String targetMenu = action.substring(6).trim();
                Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, targetMenu));
            } else if (action.startsWith("[url]")) {
                String url = action.substring(5).trim();
                player.closeInventory();
                sendClickableUrl(player, url);
            } else {
                // Ön ek yoksa varsayılan olarak oyuncu komutu çalıştır
                String cmd = action.startsWith("/") ? action.substring(1) : action;
                Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(cmd));
            }
        }
    }

    private void sendClickableUrl(Player player, String url) {
        TextComponent prefix = new TextComponent(ColorUtils.colorize("&6&l[KünefeSMP]&r &7Bağlantıyı açmak için "));
        TextComponent link = new TextComponent(ColorUtils.colorize("&b&n[BURAYA TIKLA]"));
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ColorUtils.colorize("&eTarayıcıda aç: &f" + url)).create()));
        prefix.addExtra(link);

        player.spigot().sendMessage(prefix);
    }

    public void playSound(Player player, String soundConfig) {
        if (soundConfig == null || soundConfig.isEmpty()) return;
        try {
            String[] parts = soundConfig.split(" ");
            Sound sound = Sound.valueOf(parts[0].toUpperCase());
            float vol = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
            player.playSound(player.getLocation(), sound, vol, pitch);
        } catch (Throwable ignored) {}
    }

    public MenuConfig getMenu(String id) {
        return menus.get(id.toLowerCase());
    }
}
