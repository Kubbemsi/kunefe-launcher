package com.kunefesmp.bot;

import java.util.ArrayList;
import java.util.List;

public class FaqItem {
    private final String id;
    private final List<String> rawKeywords;
    private final List<String> normalizedKeywords;
    private final String response;

    public FaqItem(String id, List<String> rawKeywords, String response) {
        this.id = id;
        this.rawKeywords = rawKeywords != null ? rawKeywords : new ArrayList<>();
        this.response = response != null ? response : "";
        this.normalizedKeywords = new ArrayList<>();
        for (String kw : this.rawKeywords) {
            String norm = KnowledgeBase.normalizeText(kw);
            if (!norm.isEmpty()) {
                this.normalizedKeywords.add(norm);
            }
        }
    }

    public String getId() {
        return id;
    }

    public List<String> getRawKeywords() {
        return rawKeywords;
    }

    public List<String> getNormalizedKeywords() {
        return normalizedKeywords;
    }

    public String getResponse() {
        return response;
    }
}
