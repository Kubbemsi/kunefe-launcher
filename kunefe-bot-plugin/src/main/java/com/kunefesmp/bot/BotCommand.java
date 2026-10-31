package com.kunefesmp.bot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class BotCommand implements CommandExecutor, TabCompleter {
    private final KunefeBot plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public BotCommand(KunefeBot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kunefebot.use")) {
            sender.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.no-permission", "&cYetkin yok!")));
            return true;
        }

        // Parametre yoksa yardım kartını göster
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        // Yönetici komutu: reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("kunefebot.admin")) {
                sender.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.no-permission", "&cYetkin yok!")));
                return true;
            }
            plugin.reloadBotConfig();
            sender.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.reload-success", "&aAyarlar yenilendi!")));
            return true;
        }

        // Yönetici komutu: status
        if (args[0].equalsIgnoreCase("status")) {
            if (!sender.hasPermission("kunefebot.admin")) {
                sender.sendMessage(plugin.getPrefix() + colorize(plugin.getConfig().getString("messages.no-permission", "&cYetkin yok!")));
                return true;
            }
            sendStatus(sender);
            return true;
        }

        // Cooldown kontrolü (oyuncu ise)
        if (sender instanceof Player player) {
            if (!player.hasPermission("kunefebot.bypass")) {
                int cdSeconds = plugin.getConfig().getInt("cooldown-seconds", 3);
                long cdMillis = cdSeconds * 1000L;
                long now = System.currentTimeMillis();
                long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

                if (now - lastUsed < cdMillis) {
                    long remainingSec = ((lastUsed + cdMillis) - now) / 1000L + 1;
                    String cdMsg = plugin.getConfig().getString("messages.cooldown", "&cBekle: {seconds} sn.")
                            .replace("{seconds}", String.valueOf(remainingSec));
                    player.sendMessage(plugin.getPrefix() + colorize(cdMsg));
                    return true;
                }
                cooldowns.put(player.getUniqueId(), now);
            }
        }

        String question = String.join(" ", args).trim();

        // 1. Önce yerel akıllı soru bankasında ara (0ms / lag yok)
        String localAnswer = plugin.getKnowledgeBase().findBestMatch(question);
        if (localAnswer != null) {
            sendResponse(sender, localAnswer);
            return true;
        }

        // 2. Yerelde bulunamadıysa ve Yapay Zeka yapılandırılmışsa AI'ya sor
        if (plugin.getAiService().isConfigured()) {
            String thinking = plugin.getConfig().getString("messages.thinking", "&eDüşünüyorum...");
            sender.sendMessage(plugin.getPrefix() + colorize(thinking));

            plugin.getAiService().askAI(question).thenAccept(aiAnswer -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (aiAnswer != null && !aiAnswer.isEmpty()) {
                        sendResponse(sender, aiAnswer);
                    } else {
                        // AI başarısız olduysa varsayılan yanıtı gönder
                        String fallback = plugin.getConfig().getString("messages.unknown-fallback", "&cBilemedim!");
                        sender.sendMessage(plugin.getPrefix() + colorize(fallback));
                    }
                });
            });
            return true;
        }

        // 3. Yapay zeka kapalıysa veya anahtar yoksa varsayılan yanıtı göster
        String fallback = plugin.getConfig().getString("messages.unknown-fallback", "&cBilemedim!");
        sender.sendMessage(plugin.getPrefix() + colorize(fallback));
        return true;
    }

    private void sendResponse(CommandSender sender, String text) {
        sender.sendMessage(plugin.getPrefix() + colorize(text));
        if (sender instanceof Player player) {
            playReplySound(player);
        }
    }

    private void playReplySound(Player player) {
        if (!plugin.getConfig().getBoolean("play-sound", true)) return;
        try {
            String soundName = plugin.getConfig().getString("sound-name", "ENTITY_EXPERIENCE_ORB_PICKUP");
            float volume = (float) plugin.getConfig().getDouble("sound-volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("sound-pitch", 1.4);
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception ignored) {
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(colorize("&6§l━━━━━━━━━━━━━━ &e§lKÜNEFE BOT &6§l━━━━━━━━━━━━━━"));
        sender.sendMessage(colorize("&7Bana sunucu veya oyun hakkında aklına takılan her şeyi sorabilirsin!"));
        sender.sendMessage(colorize("&eKullanım: &a/" + label + " <sorun>"));
        sender.sendMessage(colorize("&7Örnekler:"));
        sender.sendMessage(colorize(" &8» &f/" + label + " market nasıl açılır?"));
        sender.sendMessage(colorize(" &8» &f/" + label + " para nasıl kazanılır?"));
        sender.sendMessage(colorize(" &8» &f/" + label + " claim nasıl atılır?"));
        sender.sendMessage(colorize(" &8» &f/" + label + " evime nasıl giderim?"));
        sender.sendMessage(colorize(" &8» &f/" + label + " discord linki ne?"));
        if (sender.hasPermission("kunefebot.admin")) {
            sender.sendMessage(colorize("&cAdmin: &7/" + label + " reload &8| &7/" + label + " status"));
        }
        sender.sendMessage(colorize("&6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(colorize("&6§l[KünefeBot Durum Raporu]"));
        sender.sendMessage(colorize("&7Yüklü FAQ Soru Sayısı: &e" + plugin.getKnowledgeBase().getFaqCount()));
        boolean aiActive = plugin.getAiService().isConfigured();
        sender.sendMessage(colorize("&7Yapay Zeka (AI) Modu: " + (aiActive ? "&aAktif (" + plugin.getAiService().getProviderName() + ")" : "&cPasif (Yerel Mod)")));
    }

    private String colorize(String msg) {
        if (msg == null) return "";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("kunefebot.admin")) {
                list.add("reload");
                list.add("status");
            }
            list.add("market");
            list.add("para");
            list.add("sat");
            list.add("ah");
            list.add("claim");
            list.add("ev");
            list.add("rtp");
            list.add("spawn");
            list.add("kit");
            list.add("discord");
            list.add("vip");
        }
        return list;
    }
}
