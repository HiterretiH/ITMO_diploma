package com.logistic.backend.typedata;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.place-suggestions")
public class PlaceSuggestionProperties {

    private int maxHistory = 5;
    private int maxExternal = 10;
    private int externalMinQueryLength = 3;

    public int getMaxHistory() {
        return maxHistory;
    }

    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public int getMaxExternal() {
        return maxExternal;
    }

    public void setMaxExternal(int maxExternal) {
        this.maxExternal = maxExternal;
    }

    public int getExternalMinQueryLength() {
        return externalMinQueryLength;
    }

    public void setExternalMinQueryLength(int externalMinQueryLength) {
        this.externalMinQueryLength = externalMinQueryLength;
    }
}
