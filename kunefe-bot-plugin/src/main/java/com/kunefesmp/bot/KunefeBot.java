package com.kunefesmp.bot;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class KunefeBot extends JavaPlugin {
    private KnowledgeBase knowledgeBase;
    private AIService aiService;
    private String prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        BotCommand botCommand = new BotCommand(this);
        if (getCommand("kunefebot") != null) {
            getCommand("kunefebot").setExecutor(botCommand);
            getCommand("kunefebot").setTabCompleter(botCommand);
        }

        getLogger().info("=========================================");
        getLogger().info("  KunefeBot v1.0.0 basariyla baslatildi! ");
        getLogger().info("  Soru-Cevap Bankasi: " + knowledgeBase.getFaqCount() + " madde");
        getLogger().info("  AI Modu: " + (aiService.isConfigured() ? "Aktif (" + aiService.getProviderName() + ")" : "Pasif (Yerel Mod)"));
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("KunefeBot devre disi birakildi.");
    }

    public void reloadBotConfig() {
        reloadConfig();
        loadSettings();
    }

    private void loadSettings() {
        this.prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", "&6&l[KünefeBot]&r "));

        if (this.knowledgeBase == null) {
            this.knowledgeBase = new KnowledgeBase(this);
        }
        this.knowledgeBase.loadFromConfig(getConfig());

        if (this.aiService == null) {
            this.aiService = new AIService(this);
        }
        this.aiService.loadFromConfig(getConfig());
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public AIService getAiService() {
        return aiService;
    }

    public String getPrefix() {
        return prefix;
    }
}
