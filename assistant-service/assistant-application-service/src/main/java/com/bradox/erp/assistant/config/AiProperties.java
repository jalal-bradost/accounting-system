package com.bradox.erp.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI assistant configuration ({@code app.ai.*}) — YAML defaults; Settings UI may override per company.
 */
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** Default master switch when no DB settings row exists. */
    private boolean enabled = false;

    /** Default mode when no DB row: hybrid | google | openai | ollama */
    private String provider = "hybrid";

    private final Ollama ollama = new Ollama();
    private final Google google = new Google();
    private final OpenAi openai = new OpenAi();

    private int maxToolRounds = 4;
    private int maxHistoryMessages = 12;
    private int maxDateRangeDays = 366;
    private boolean writeOperationsEnabled = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Ollama getOllama() { return ollama; }
    public Google getGoogle() { return google; }
    public OpenAi getOpenai() { return openai; }
    public int getMaxToolRounds() { return maxToolRounds; }
    public void setMaxToolRounds(int maxToolRounds) { this.maxToolRounds = maxToolRounds; }
    public int getMaxHistoryMessages() { return maxHistoryMessages; }
    public void setMaxHistoryMessages(int maxHistoryMessages) { this.maxHistoryMessages = maxHistoryMessages; }
    public int getMaxDateRangeDays() { return maxDateRangeDays; }
    public void setMaxDateRangeDays(int maxDateRangeDays) { this.maxDateRangeDays = maxDateRangeDays; }
    public boolean isWriteOperationsEnabled() { return writeOperationsEnabled; }
    public void setWriteOperationsEnabled(boolean writeOperationsEnabled) {
        this.writeOperationsEnabled = writeOperationsEnabled;
    }

    public static class Ollama {
        private boolean enabled = true;
        private String baseUrl = "http://localhost:11434";
        private String model = "qwen2.5:3b";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 120000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }

    public static class Google {
        private boolean enabled = false;
        private String apiKey = "";
        private String model = "gemini-3.8-flash";
        /** OpenAI-compatible Gemini endpoint (Google AI Studio). */
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 120000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }

    public static class OpenAi {
        private boolean enabled = false;
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private String baseUrl = "https://api.openai.com/v1";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 120000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }
}
