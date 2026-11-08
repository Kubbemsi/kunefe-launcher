package com.kunefesmp.bot;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class KnowledgeBase {
    private final KunefeBot plugin;
    private final List<FaqItem> faqItems = new ArrayList<>();
    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr-TR");
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "mi", "mu", "mı", "mü", "ne", "ve", "ile", "de", "da", "bu", "o", "su", "bir", "icin", "nasil", "var"
    ));

    public KnowledgeBase(KunefeBot plugin) {
        this.plugin = plugin;
    }

    public void loadFromConfig(FileConfiguration config) {
        faqItems.clear();
        ConfigurationSection section = config.getConfigurationSection("faq");
        if (section == null) {
            plugin.getLogger().warning("config.yml içinde 'faq' bölümü bulunamadı!");
            return;
        }

        for (String key : section.getKeys(false)) {
            List<String> keywords = section.getStringList(key + ".keywords");
            String response = section.getString(key + ".response", "");
            if (!keywords.isEmpty() && !response.isEmpty()) {
                faqItems.add(new FaqItem(key, keywords, response));
            }
        }
        plugin.getLogger().info("Toplam " + faqItems.size() + " adet Soru-Cevap (FAQ) maddesi yüklendi.");
    }

    public int getFaqCount() {
        return faqItems.size();
    }

    /**
     * Soruya en uygun yerel yanıtı arar. Bulunamazsa null döner (AI devreye girer).
     */
    public String findBestMatch(String rawQuestion) {
        if (rawQuestion == null || rawQuestion.trim().isEmpty()) {
            return null;
        }

        String normQ = normalizeText(rawQuestion);
        Set<String> qWords = new HashSet<>(Arrays.asList(normQ.split("\\s+")));

        FaqItem bestItem = null;
        int highestScore = 0;

        for (FaqItem item : faqItems) {
            int score = calculateScore(normQ, qWords, item);
            if (score > highestScore) {
                highestScore = score;
                bestItem = item;
            }
        }

        // Güven eşiği: en az 60 puan olmalı
        if (bestItem != null && highestScore >= 60) {
            return bestItem.getResponse();
        }

        return null;
    }

    private int calculateScore(String normQuestion, Set<String> qWords, FaqItem item) {
        int score = 0;

        for (String kw : item.getNormalizedKeywords()) {
            if (kw.isEmpty()) continue;

            // Tam veya kısmi kalıp eşleşmesi
            if (normQuestion.equals(kw)) {
                score += 100;
            } else if (normQuestion.contains(kw) && kw.length() >= 4) {
                score += 40 + (kw.length() * 2);
            }

            // Kelime bazlı eşleşme
            String[] kwParts = kw.split("\\s+");
            int matchedParts = 0;
            for (String part : kwParts) {
                if (part.length() < 3 || STOP_WORDS.contains(part)) {
                    if (qWords.contains(part)) {
                        matchedParts++;
                    }
                    continue;
                }

                if (qWords.contains(part)) {
                    matchedParts++;
                } else {
                    for (String qw : qWords) {
                        if (qw.startsWith(part) || (part.length() >= 5 && qw.contains(part))) {
                            matchedParts++;
                            break;
                        }
                    }
                }
            }

            if (matchedParts > 0) {
                if (matchedParts == kwParts.length) {
                    score += 50 + (matchedParts * 10);
                } else if (kwParts.length > 1 && (float) matchedParts / kwParts.length >= 0.5f) {
                    score += matchedParts * 15;
                }
            }
        }

        return score;
    }

    /**
     * Türkçe karakterleri sadeleştirir, küçük harfe çevirir ve noktalama işaretlerini kaldırır.
     */
    public static String normalizeText(String input) {
        if (input == null) return "";

        String s = input.toLowerCase(TR_LOCALE);

        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'ç': sb.append('c'); break;
                case 'ğ': sb.append('g'); break;
                case 'ı':
                case 'i':
                case 'İ': sb.append('i'); break;
                case 'ö': sb.append('o'); break;
                case 'ş': sb.append('s'); break;
                case 'ü': sb.append('u'); break;
                default:
                    if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                        sb.append(c);
                    } else {
                        sb.append(' ');
                    }
                    break;
            }
        }

        return sb.toString().trim().replaceAll("\\s+", " ");
    }
}
