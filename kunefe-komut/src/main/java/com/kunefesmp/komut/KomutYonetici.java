package com.kunefesmp.komut;

import org.bukkit.Bukkit;
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
    private final SpawnManager spawnManager;

    public KomutYonetici(KunefeKomut plugin, ItemDataHelper helper, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.helper = helper;
        this.spawnManager = spawnManager;
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
            spawnManager.loadFromConfig();
            sender.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.reload", "&aAyarlar yenilendi!")));
            return true;
        }

        if (sub.equals("spawnesyasi") || sub.equals("spawnesya") || sub.equals("spawnitem")) {
            handleSpawnItem(sender, label, args);
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
            case "sadece-spawn", "sadespawn", "spawnonly" -> {
                boolean currentState = helper.isSpawnOnly(item);
                boolean newState;
                if (args.length >= 2) {
                    String opt = args[1].toLowerCase(Locale.ROOT);
                    newState = opt.equals("ac") || opt.equals("true") || opt.equals("evet") || opt.equals("1");
                } else {
                    newState = !currentState;
                }
                helper.setSpawnOnly(item, newState);
                if (newState) {
                    player.sendMessage(plugin.getPrefix() + colorize("&a[✔] Bu eşya artık &6SADECE SPAWN BÖLGESİNDE &açalışacak şekilde ayarlandı!"));
                } else {
                    player.sendMessage(plugin.getPrefix() + colorize("&e[✔] Spawn kısıtlaması kaldırıldı; eşya her yerde kullanılabilir."));
                }
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
                player.sendMessage(colorize("&7Sadece Spawn'da mı?: &f" + (helper.isSpawnOnly(item) ? "&aEVET" : "&7HAYIR")));
                player.sendMessage(colorize("&7Tek Kullanımlık mı?: &f" + (helper.isConsume(item) ? "&cEVET" : "&aHAYIR")));
                player.sendMessage(colorize("&7Özel Bekleme Süresi: &f" + helper.getCooldown(item) + " sn"));
                player.sendMessage(colorize("&6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            }
            default -> sendHelp(sender, label);
        }

        return true;
    }

    private void handleSpawnItem(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize("&6§l[KünefeKomut Spawn Eşyası Yönetimi]"));
            sender.sendMessage(colorize(" &8» &f/" + label + " spawnesyasi kaydet [slot 0-8] &7- Elindeki eşyayı otomatik spawn eşyası yapar"));
            sender.sendMessage(colorize(" &8» &f/" + label + " spawnesyasi sil &7- Otomatik spawn eşyasını kaldırır"));
            sender.sendMessage(colorize(" &8» &f/" + label + " spawnesyasi ver [oyuncu] &7- Spawn eşyasını oyuncuya verir"));
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);

        if (action.equals("kaydet")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Bu komut sadece oyuncular tarafından kullanılabilir.");
                return;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                player.sendMessage(plugin.getPrefix() + colorize("&c[!] Elinde kaydedilecek bir eşya tutmalısın!"));
                return;
            }
            int slot = spawnManager.getPreferredSlot();
            if (args.length >= 3) {
                try {
                    slot = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {
                }
            }

            spawnManager.setSpawnItem(item, slot);
            player.sendMessage(plugin.getPrefix() + colorize("&a[✔] Elindeki eşya sunucunun &6OTOMATİK SPAWN EŞYASI &aolarak kaydedildi! (Yuva: &e" + slot + "&a)"));
            player.sendMessage(plugin.getPrefix() + colorize("&7Spawn bölgesine gelen herkesin eline bu eşya verilecek, spawn'dan çıkınca otomatik silinecektir."));
        } else if (action.equals("sil")) {
            spawnManager.setSpawnItem(null, 4);
            sender.sendMessage(plugin.getPrefix() + colorize("&c[✔] Otomatik spawn eşyası kaldırıldı ve oyunculardan temizlendi."));
        } else if (action.equals("ver")) {
            if (!spawnManager.hasSpawnItemConfigured()) {
                sender.sendMessage(plugin.getPrefix() + colorize("&cHenüz bir spawn eşyası kaydedilmemiş! Önce /" + label + " spawnesyasi kaydet kullanın."));
                return;
            }
            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[2]);
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage("Konsoldan verirken hedef oyuncu belirtmelisiniz.");
                return;
            }

            if (target != null && target.isOnline()) {
                spawnManager.giveSpawnItem(target);
                sender.sendMessage(plugin.getPrefix() + colorize("&aSpawn eşyası &e" + target.getName() + " &aoyuncusuna verildi."));
            } else {
                sender.sendMessage(plugin.getPrefix() + colorize("&cOyuncu bulunamadı!"));
            }
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(colorize("&6§l━━━━━━━━━━━━━━ &e§lKÜNEFE KOMUT &6§l━━━━━━━━━━━━━━"));
        sender.sendMessage(colorize("&7Eline bir eşya alıp aşağıdaki komutları kullanabilirsin:"));
        sender.sendMessage(colorize(" &8» &a/" + label + " ekle <komut> &7- Oyuncu adına komut bağlar"));
        sender.sendMessage(colorize(" &8» &6/" + label + " konsol <komut> &7- Konsol adına komut bağlar (Örn ödül/para)"));
        sender.sendMessage(colorize(" &8» &e/" + label + " sadece-spawn [ac|kapat] &7- Sadece spawn bölgesinde çalışsın"));
        sender.sendMessage(colorize(" &8» &6/" + label + " spawnesyasi kaydet [slot] &7- Spawndayken otomatik ver, çıkınca sil!"));
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
            list.addAll(Arrays.asList("ekle", "konsol", "sadece-spawn", "spawnesyasi", "tekkullanimlik", "cooldown", "isim", "lore", "sil", "bilgi", "reload"));
            return filter(list, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("tekkullanimlik") || sub.equals("tek") || sub.equals("sadece-spawn") || sub.equals("sadespawn")) {
                list.addAll(Arrays.asList("ac", "kapat"));
            } else if (sub.equals("spawnesyasi") || sub.equals("spawnesya")) {
                list.addAll(Arrays.asList("kaydet", "sil", "ver"));
            } else if (sub.equals("lore")) {
                list.addAll(Arrays.asList("ekle", "temizle"));
            } else if (sub.equals("ekle") || sub.equals("konsol")) {
                list.addAll(Arrays.asList("shop", "menu", "spawn", "rtp", "kit", "discord", "ayarlar", "warp"));
            }
            return filter(list, args[1]);
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("spawnesyasi") || args[0].equalsIgnoreCase("spawnesya"))) {
            if (args[1].equalsIgnoreCase("kaydet")) {
                list.addAll(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8"));
            } else if (args[1].equalsIgnoreCase("ver")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }
            }
            return filter(list, args[2]);
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
