package com.kunefesmp.bot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.file.FileConfiguration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class AIService {
    private final KunefeBot plugin;
    private final HttpClient httpClient;

    private boolean enabled;
    private String provider;
    private String geminiApiKey;
    private String geminiModel;
    private String openaiApiKey;
    private String openaiModel;
    private String openaiEndpoint;
    private String systemPrompt;
    private int timeoutSeconds;

    public AIService(KunefeBot plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void loadFromConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("ai.enabled", true);
        this.provider = config.getString("ai.provider", "gemini").toLowerCase();

        this.geminiApiKey = config.getString("ai.gemini.api-key", "").trim();
        this.geminiModel = config.getString("ai.gemini.model", "gemini-1.5-flash").trim();

        this.openaiApiKey = config.getString("ai.openai.api-key", "").trim();
        this.openaiModel = config.getString("ai.openai.model", "gpt-4o-mini").trim();
        this.openaiEndpoint = config.getString("ai.openai.endpoint", "https://api.openai.com/v1/chat/completions").trim();

        this.systemPrompt = config.getString("ai.system-prompt", "Sen KünefeSMP sunucusunun yardımcı botusun.").trim();
        this.timeoutSeconds = config.getInt("ai.timeout-seconds", 6);
    }

    public boolean isConfigured() {
        if (!enabled) return false;
        if ("gemini".equalsIgnoreCase(provider)) {
            return !geminiApiKey.isEmpty() && !geminiApiKey.startsWith("BURAYA");
        } else if ("openai".equalsIgnoreCase(provider)) {
            return !openaiApiKey.isEmpty() && !openaiApiKey.startsWith("BURAYA");
        }
        return false;
    }

    public String getProviderName() {
        return provider;
    }

    /**
     * Asenkron olarak yapay zekaya sorar.
     */
    public CompletableFuture<String> askAI(String question) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(null);
        }

        if ("openai".equalsIgnoreCase(provider)) {
            return askOpenAI(question);
        } else {
            return askGemini(question);
        }
    }

    private CompletableFuture<String> askGemini(String question) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + geminiModel + ":generateContent?key=" + geminiApiKey;

                JsonObject root = new JsonObject();

                // systemInstruction
                JsonObject sysObj = new JsonObject();
                JsonArray sysParts = new JsonArray();
                JsonObject sysTextPart = new JsonObject();
                sysTextPart.addProperty("text", systemPrompt);
                sysParts.add(sysTextPart);
                sysObj.add("parts", sysParts);
                root.add("systemInstruction", sysObj);

                // contents
                JsonArray contents = new JsonArray();
                JsonObject userMessage = new JsonObject();
                userMessage.addProperty("role", "user");
                JsonArray userParts = new JsonArray();
                JsonObject userTextPart = new JsonObject();
                userTextPart.addProperty("text", question);
                userParts.add(userTextPart);
                userMessage.add("parts", userParts);
                contents.add(userMessage);
                root.add("contents", contents);

                // generationConfig
                JsonObject genConfig = new JsonObject();
                genConfig.addProperty("maxOutputTokens", 200);
                genConfig.addProperty("temperature", 0.4);
                root.add("generationConfig", genConfig);

                String requestBody = root.toString();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                        JsonObject content = firstCandidate.getAsJsonObject("content");
                        if (content != null) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts != null && !parts.isEmpty()) {
                                String reply = parts.get(0).getAsJsonObject().get("text").getAsString();
                                return cleanAiReply(reply);
                            }
                        }
                    }
                } else {
                    plugin.getLogger().warning("Gemini API hatası (" + response.statusCode() + "): " + response.body());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Gemini API bağlantı hatası: " + e.getMessage());
            }
            return null;
        });
    }

    private CompletableFuture<String> askOpenAI(String question) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject root = new JsonObject();
                root.addProperty("model", openaiModel);
                root.addProperty("max_tokens", 150);
                root.addProperty("temperature", 0.4);

                JsonArray messages = new JsonArray();

                JsonObject sysMsg = new JsonObject();
                sysMsg.addProperty("role", "system");
                sysMsg.addProperty("content", systemPrompt);
                messages.add(sysMsg);

                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", question);
                messages.add(userMsg);

                root.add("messages", messages);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(openaiEndpoint))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + openaiApiKey)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonArray choices = jsonResponse.getAsJsonArray("choices");
                    if (choices != null && !choices.isEmpty()) {
                        JsonObject firstChoice = choices.get(0).getAsJsonObject();
                        JsonObject message = firstChoice.getAsJsonObject("message");
                        if (message != null && message.has("content")) {
                            String reply = message.get("content").getAsString();
                            return cleanAiReply(reply);
                        }
                    }
                } else {
                    plugin.getLogger().warning("OpenAI API hatası (" + response.statusCode() + "): " + response.body());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("OpenAI API bağlantı hatası: " + e.getMessage());
            }
            return null;
        });
    }

    private String cleanAiReply(String reply) {
        if (reply == null) return null;
        String cleaned = reply.trim();
        // Eğer AI markdown tırnakları ya da fazla satır sonu eklediyse temizle
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned.replace("\n\n", " ").replace("\n", " ");
    }
}
