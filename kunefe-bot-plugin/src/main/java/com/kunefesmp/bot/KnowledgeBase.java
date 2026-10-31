package com.kunefesmp.bot;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class KnowledgeBase {
    private final KunefeBot plugin;
    private final List<FaqItem> faqItems = new ArrayList<>();
    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr-TR");

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
     * Soruya en uygun yerel yanıtı arar. Bulunamazsa null döner.
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

        // Güven eşiği: en az 20 puan
        if (bestItem != null && highestScore >= 20) {
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
            } else if (normQuestion.contains(kw)) {
                // Soru içinde kalıp geçiyorsa kelime uzunluğuna göre puan
                score += 30 + (kw.length() * 2);
            }

            // Kelime bazlı eşleşme
            String[] kwParts = kw.split("\\s+");
            int matchedParts = 0;
            for (String part : kwParts) {
                if (part.length() < 2) continue;
                if (qWords.contains(part)) {
                    matchedParts++;
                } else {
                    // Kısmi kök kontrolü (Türkçe ekler için örn: "markete", "pazarda", "evime")
                    for (String qw : qWords) {
                        if (qw.startsWith(part) || (part.length() >= 4 && qw.contains(part))) {
                            matchedParts++;
                            break;
                        }
                    }
                }
            }

            if (matchedParts > 0) {
                score += (matchedParts * 15);
                if (matchedParts == kwParts.length) {
                    score += 25; // Anahtar kelimenin tüm parçaları geçti
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

        // Fazla boşlukları temizle
        return sb.toString().trim().replaceAll("\\s+", " ");
    }
}
