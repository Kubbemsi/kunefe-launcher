package com.kunefesmp.menu.command;

import com.kunefesmp.menu.KunefeMenu;
import com.kunefesmp.menu.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuCommand implements CommandExecutor, TabCompleter {

    private final KunefeMenu plugin;

    public MenuCommand(KunefeMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        // /kunefemenu admin komutları
        if (cmdName.equals("kunefemenu")) {
            if (args.length > 0) {
                String sub = args[0].toLowerCase();
                if (sub.equals("reload")) {
                    if (!sender.hasPermission("kunefemenu.admin")) {
                        sender.sendMessage(ColorUtils.colorize("&cBu komutu kullanmak için yetkiniz yok!"));
                        return true;
                    }
                    plugin.reloadPlugin();
                    sender.sendMessage(ColorUtils.colorize("&a&l[KünefeMenu]&r &aYapılandırma ve menüler başarıyla yenilendi!"));
                    return true;
                } else if (sub.equals("ac") || sub.equals("open")) {
                    if (!sender.hasPermission("kunefemenu.admin")) {
                        sender.sendMessage(ColorUtils.colorize("&cBu komutu kullanmak için yetkiniz yok!"));
                        return true;
                    }
                    if (args.length < 2) {
                        sender.sendMessage(ColorUtils.colorize("&cKullanım: /kunefemenu ac <oyuncu> [menü-adı]"));
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ColorUtils.colorize("&cOyuncu bulunamadı: " + args[1]));
                        return true;
                    }
                    String menuName = args.length > 2 ? args[2] : "main";
                    plugin.getMenuManager().openMenu(target, menuName);
                    sender.sendMessage(ColorUtils.colorize("&a" + target.getName() + " için '" + menuName + "' menüsü açıldı."));
                    return true;
                } else if (sub.equals("yardim") || sub.equals("help")) {
                    sender.sendMessage(ColorUtils.colorize("&6━━━━━━━━━ KÜNEFE MENU YARDIM ━━━━━━━━━"));
                    sender.sendMessage(ColorUtils.colorize("&e/menu &7- Sunucu ana menüsünü açar"));
                    sender.sendMessage(ColorUtils.colorize("&e/kunefemenu reload &7- Menü ayarlarını yeniler"));
                    sender.sendMessage(ColorUtils.colorize("&e/kunefemenu ac <oyuncu> [menü] &7- Bir oyuncuya menü açar"));
                    sender.sendMessage(ColorUtils.colorize("&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                    return true;
                }
            }
        }

        // Oyuncu menü açma komutu (/menu, /menü, /sunucu, /kurallar vb.)
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize("&cBu komut yalnızca oyun içinden kullanılabilir! (Konsol için: /kunefemenu ac <oyuncu>)"));
            return true;
        }

        if (cmdName.equals("kurallar") || cmdName.equals("kural") || cmdName.equals("rules")) {
            plugin.getMenuManager().openMenu(player, "kurallar");
            return true;
        }

        String targetMenu = (args.length > 0) ? args[0] : "main";
        plugin.getMenuManager().openMenu(player, targetMenu);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("kunefemenu.admin")) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("reload");
            list.add("ac");
            list.add("yardim");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("ac")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("ac")) {
            list.add("main");
        }
        return list;
    }
}
