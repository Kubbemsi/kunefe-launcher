package com.kunefesmp.bot;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Locale;

public class ChatListener implements Listener {
    private final KunefeBot plugin;

    public ChatListener(KunefeBot plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-mention.enabled", true)) {
            return;
        }

        String message = event.getMessage().trim();
        String lower = message.toLowerCase(Locale.forLanguageTag("tr-TR"));

        String question = null;
        if (lower.startsWith("@bot ") || lower.startsWith("@künefebot ") || lower.startsWith("@kunefebot ")) {
            int firstSpace = message.indexOf(' ');
            if (firstSpace != -1) {
                question = message.substring(firstSpace + 1).trim();
            }
        } else if (lower.startsWith("bot, ") || lower.startsWith("bot: ") || lower.startsWith("bot ")) {
            int firstSpace = message.indexOf(' ');
            if (firstSpace != -1) {
                question = message.substring(firstSpace + 1).trim();
            }
        }

        if (question == null || question.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        final String finalQuestion = question;
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getBotCommand().handleQuestion(player, finalQuestion);
        });
    }
}
