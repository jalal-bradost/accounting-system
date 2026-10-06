package com.bradox.erp.assistant.settings;

/**
 * Update payload from Settings UI.
 * API keys: null/omit = keep existing; use clear* flags to remove; non-blank = replace.
 */
public class AssistantSettingsUpdateRequest {

    private Boolean assistantEnabled;
    private String mode;
    private Boolean googleEnabled;
    private String googleApiKey;
    private Boolean clearGoogleApiKey;
    private String googleModel;
    private Boolean openAiEnabled;
    private String openAiApiKey;
    private Boolean clearOpenAiApiKey;
    private String openAiModel;
    private Boolean ollamaEnabled;
    private String ollamaBaseUrl;
    private String ollamaModel;

    public Boolean getAssistantEnabled() { return assistantEnabled; }
    public void setAssistantEnabled(Boolean assistantEnabled) { this.assistantEnabled = assistantEnabled; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Boolean getGoogleEnabled() { return googleEnabled; }
    public void setGoogleEnabled(Boolean googleEnabled) { this.googleEnabled = googleEnabled; }
    public String getGoogleApiKey() { return googleApiKey; }
    public void setGoogleApiKey(String googleApiKey) { this.googleApiKey = googleApiKey; }
    public Boolean getClearGoogleApiKey() { return clearGoogleApiKey; }
    public void setClearGoogleApiKey(Boolean clearGoogleApiKey) { this.clearGoogleApiKey = clearGoogleApiKey; }
    public String getGoogleModel() { return googleModel; }
    public void setGoogleModel(String googleModel) { this.googleModel = googleModel; }
    public Boolean getOpenAiEnabled() { return openAiEnabled; }
    public void setOpenAiEnabled(Boolean openAiEnabled) { this.openAiEnabled = openAiEnabled; }
    public String getOpenAiApiKey() { return openAiApiKey; }
    public void setOpenAiApiKey(String openAiApiKey) { this.openAiApiKey = openAiApiKey; }
    public Boolean getClearOpenAiApiKey() { return clearOpenAiApiKey; }
    public void setClearOpenAiApiKey(Boolean clearOpenAiApiKey) { this.clearOpenAiApiKey = clearOpenAiApiKey; }
    public String getOpenAiModel() { return openAiModel; }
    public void setOpenAiModel(String openAiModel) { this.openAiModel = openAiModel; }
    public Boolean getOllamaEnabled() { return ollamaEnabled; }
    public void setOllamaEnabled(Boolean ollamaEnabled) { this.ollamaEnabled = ollamaEnabled; }
    public String getOllamaBaseUrl() { return ollamaBaseUrl; }
    public void setOllamaBaseUrl(String ollamaBaseUrl) { this.ollamaBaseUrl = ollamaBaseUrl; }
    public String getOllamaModel() { return ollamaModel; }
    public void setOllamaModel(String ollamaModel) { this.ollamaModel = ollamaModel; }
}
