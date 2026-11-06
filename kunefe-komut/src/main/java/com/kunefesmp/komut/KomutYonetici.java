package com.kunefesmp.komut;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class KomutYonetici implements CommandExecutor, TabCompleter {
    private final KunefeKomut plugin;
    private final ItemDataHelper helper;

    public KomutYonetici(KunefeKomut plugin, ItemDataHelper helper) {
        this.plugin = plugin;
        this.helper = helper;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kunefekomut.admin")) {
            sender.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.no-permission", "&cYetkin yok!")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            plugin.reloadPluginConfig();
            sender.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.reload", "&aAyarlar yenilendi!")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut sadece oyun içindeki oyuncular tarafından kullanılabilir.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.no-item", "&c[!] Elinde bir eşya tutmalısın!")));
            return true;
        }

        switch (sub) {
            case "ekle" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getPrefix() + colorize("&eKullanım: &f/" + label + " ekle <komut>"));
                    return true;
                }
                String cmd = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
                helper.bindCommand(item, cmd, ItemDataHelper.CommandType.PLAYER);
                String msg = plugin.getConfig().getString("messages.command-added-player", "&aKomut bağlandı: &e/{cmd}")
                        .replace("{cmd}", cmd);
                player.sendMessage(plugin.getPrefix() + colorize(msg));
            }
            case "konsol" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getPrefix() + colorize("&eKullanım: &f/" + label + " konsol <komut>"));
                    return true;
                }
                String cmd = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
                helper.bindCommand(item, cmd, ItemDataHelper.CommandType.CONSOLE);
                String msg = plugin.getConfig().getString("messages.command-added-console", "&aKonsol komutu bağlandı: &6/{cmd}")
                        .replace("{cmd}", cmd);
                player.sendMessage(plugin.getPrefix() + colorize(msg));
            }
            case "tekkullanimlik", "tek" -> {
                boolean currentState = helper.isConsume(item);
                boolean newState;
                if (args.length >= 2) {
                    String opt = args[1].toLowerCase(Locale.ROOT);
                    newState = opt.equals("ac") || opt.equals("true") || opt.equals("evet") || opt.equals("1");
                } else {
                    newState = !currentState;
                }
                helper.setConsume(item, newState);
                if (newState) {
                    player.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.single-use-on", "&eEşya artık TEK KULLANIMLIK!")));
                } else {
                    player.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.single-use-off", "&eEşya artık SÜREKLİ KULLANILABİLİR!")));
                }
            }
            case "cooldown", "bekleme" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getPrefix() + colorize("&eKullanım: &f/" + label + " cooldown <saniye>"));
                    return true;
                }
                try {
                    int cd = Integer.parseInt(args[1]);
                    helper.setCooldown(item, cd);
                    String msg = plugin.getConfig().getString("messages.cooldown-set", "&aBekleme süresi: {seconds} sn.")
                            .replace("{seconds}", String.valueOf(cd));
                    player.sendMessage(plugin.getPrefix() + colorize(msg));
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getPrefix() + colorize("&cLütfen geçerli bir sayı girin!"));
                }
            }
            case "isim", "ad" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getPrefix() + colorize("&eKullanım: &f/" + label + " isim <yeni isim>"));
                    return true;
                }
                String newName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                helper.setDisplayName(item, newName);
                player.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.name-set", "&aİsim güncellendi!")));
            }
            case "lore" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getPrefix() + colorize("&eKullanım: &f/" + label + " lore <ekle|temizle> [yazı]"));
                    return true;
                }
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("temizle") || action.equals("sil")) {
                    helper.clearAllLore(item);
                    player.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.lore-cleared", "&eAçıklamalar temizlendi.")));
                } else if (action.equals("ekle")) {
                    if (args.length < 3) {
                        player.sendMessage(plugin.getPrefix() + colorize("&eKullanım: &f/" + label + " lore ekle <yazı>"));
                        return true;
                    }
                    String line = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    helper.addCustomLore(item, line);
                    player.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.lore-added", "&aAçıklama eklendi!")));
                }
            }
            case "sil", "kaldir" -> {
                if (!helper.hasCommand(item)) {
                    player.sendMessage(plugin.getPrefix() + colorize("&eBu eşyada zaten atanmış bir komut yok."));
                    return true;
                }
                helper.clearCommand(item);
                player.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.command-removed", "&cEşyadaki komut temizlendi.")));
            }
            case "bilgi", "info" -> {
                if (!helper.hasCommand(item)) {
                    player.sendMessage(plugin.getPrefix() + colorize("&eBu eşyada kayıtlı bir komut bulunmuyor."));
                    return true;
                }
                player.sendMessage(colorize("&6§l━━━━━━━━━ &e§lEŞYA KOMUT BİLGİSİ &6§l━━━━━━━━━"));
                player.sendMessage(colorize("&7Komut: &f/" + helper.getCommand(item)));
                player.sendMessage(colorize("&7Çalışma Türü: &f" + (helper.getType(item) == ItemDataHelper.CommandType.CONSOLE ? "&cKONSOL (Admin)" : "&aOYUNCU")));
                player.sendMessage(colorize("&7Tek Kullanımlık mı?: &f" + (helper.isConsume(item) ? "&cEVET" : "&aHAYIR")));
                player.sendMessage(colorize("&7Özel Bekleme Süresi: &f" + helper.getCooldown(item) + " sn"));
                player.sendMessage(colorize("&6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            }
            default -> sendHelp(sender, label);
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(colorize("&6§l━━━━━━━━━━━━━━ &e§lKÜNEFE KOMUT &6§l━━━━━━━━━━━━━━"));
        sender.sendMessage(colorize("&7Eline bir eşya alıp aşağıdaki komutları kullanabilirsin:"));
        sender.sendMessage(colorize(" &8» &a/" + label + " ekle <komut> &7- Oyuncu adına komut bağlar"));
        sender.sendMessage(colorize(" &8» &6/" + label + " konsol <komut> &7- Konsol adına komut bağlar (Örn ödül/para)"));
        sender.sendMessage(colorize(" &8» &e/" + label + " tekkullanimlik [ac|kapat] &7- Kullanılınca eşya silinsin mi?"));
        sender.sendMessage(colorize(" &8» &b/" + label + " cooldown <sn> &7- Eşyaya tıklama bekleme süresi koyar"));
        sender.sendMessage(colorize(" &8» &d/" + label + " isim <yeni isim> &7- Eşyanın adını renklendirir"));
        sender.sendMessage(colorize(" &8» &f/" + label + " lore <ekle|temizle> [yazı] &7- Eşyaya açıklama ekler"));
        sender.sendMessage(colorize(" &8» &c/" + label + " sil &7- Eşyadaki komutu ve açıklamaları temizler"));
        sender.sendMessage(colorize(" &8» &7/" + label + " bilgi &7- Eşyadaki kayıtlı komut bilgilerini gösterir"));
        sender.sendMessage(colorize(" &8» &7/" + label + " reload &7- Ayarları yeniler"));
        sender.sendMessage(colorize("&6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private String colorize(String msg) {
        if (msg == null) return "";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (!sender.hasPermission("kunefekomut.admin")) return list;

        if (args.length == 1) {
            list.addAll(Arrays.asList("ekle", "konsol", "tekkullanimlik", "cooldown", "isim", "lore", "sil", "bilgi", "reload"));
            return filter(list, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("tekkullanimlik") || sub.equals("tek")) {
                list.addAll(Arrays.asList("ac", "kapat"));
            } else if (sub.equals("lore")) {
                list.addAll(Arrays.asList("ekle", "temizle"));
            } else if (sub.equals("ekle") || sub.equals("konsol")) {
                list.addAll(Arrays.asList("shop", "menu", "spawn", "rtp", "kit", "discord", "ayarlar", "warp"));
            }
            return filter(list, args[1]);
        }

        return list;
    }

    private List<String> filter(List<String> list, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> res = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase(Locale.ROOT).startsWith(p)) {
                res.add(s);
            }
        }
        return res;
    }
}
