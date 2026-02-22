package com.suarez.expenses.plaid;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "plaid")
public class PlaidProperties {

    private boolean enabled;
    private String baseUrl = "https://sandbox.plaid.com";
    private String clientId;
    private String secret;
    private String clientName = "Expense Manager";
    private String language = "en";
    private List<String> countryCodes = new ArrayList<>(List.of("US"));
    private List<String> products = new ArrayList<>(List.of("transactions"));
    private String webhook;
    private String redirectUri;
    private int syncPageSize = 100;
    private final Usage usage = new Usage();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getCountryCodes() {
        return countryCodes;
    }

    public void setCountryCodes(List<String> countryCodes) {
        if (countryCodes == null || countryCodes.isEmpty()) {
            this.countryCodes = new ArrayList<>(List.of("US"));
            return;
        }
        this.countryCodes = new ArrayList<>(countryCodes);
    }

    public List<String> getProducts() {
        return products;
    }

    public void setProducts(List<String> products) {
        if (products == null || products.isEmpty()) {
            this.products = new ArrayList<>(List.of("transactions"));
            return;
        }
        this.products = new ArrayList<>(products);
    }

    public String getWebhook() {
        return webhook;
    }

    public void setWebhook(String webhook) {
        this.webhook = webhook;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public int getSyncPageSize() {
        return syncPageSize;
    }

    public void setSyncPageSize(int syncPageSize) {
        this.syncPageSize = syncPageSize;
    }

    public Usage getUsage() {
        return usage;
    }

    public static class Usage {
        private int freeMonthlyCallLimit = 200;
        private int warningThresholdPercent = 80;

        public int getFreeMonthlyCallLimit() {
            return freeMonthlyCallLimit;
        }

        public void setFreeMonthlyCallLimit(int freeMonthlyCallLimit) {
            this.freeMonthlyCallLimit = freeMonthlyCallLimit;
        }

        public int getWarningThresholdPercent() {
            return warningThresholdPercent;
        }

        public void setWarningThresholdPercent(int warningThresholdPercent) {
            this.warningThresholdPercent = warningThresholdPercent;
        }
    }
}
