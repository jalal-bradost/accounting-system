package com.bradox.erp.assistant.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_assistant_settings")
public class AssistantSettingsEntity {

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "assistant_enabled", nullable = false)
    private boolean assistantEnabled;

    /** hybrid | google | openai | ollama */
    @Column(name = "mode", nullable = false, length = 32)
    private String mode = "hybrid";

    @Column(name = "google_enabled", nullable = false)
    private boolean googleEnabled;

    @Column(name = "google_api_key", length = 512)
    private String googleApiKey;

    @Column(name = "google_model", nullable = false, length = 128)
    private String googleModel = "gemini-3.8-flash";

    @Column(name = "openai_enabled", nullable = false)
    private boolean openAiEnabled;

    @Column(name = "openai_api_key", length = 512)
    private String openAiApiKey;

    @Column(name = "openai_model", nullable = false, length = 128)
    private String openAiModel = "gpt-4o-mini";

    @Column(name = "ollama_enabled", nullable = false)
    private boolean ollamaEnabled = true;

    @Column(name = "ollama_base_url", nullable = false, length = 512)
    private String ollamaBaseUrl = "http://localhost:11434";

    @Column(name = "ollama_model", nullable = false, length = 128)
    private String ollamaModel = "qwen2.5:3b";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public boolean isAssistantEnabled() { return assistantEnabled; }
    public void setAssistantEnabled(boolean assistantEnabled) { this.assistantEnabled = assistantEnabled; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isGoogleEnabled() { return googleEnabled; }
    public void setGoogleEnabled(boolean googleEnabled) { this.googleEnabled = googleEnabled; }
    public String getGoogleApiKey() { return googleApiKey; }
    public void setGoogleApiKey(String googleApiKey) { this.googleApiKey = googleApiKey; }
    public String getGoogleModel() { return googleModel; }
    public void setGoogleModel(String googleModel) { this.googleModel = googleModel; }
    public boolean isOpenAiEnabled() { return openAiEnabled; }
    public void setOpenAiEnabled(boolean openAiEnabled) { this.openAiEnabled = openAiEnabled; }
    public String getOpenAiApiKey() { return openAiApiKey; }
    public void setOpenAiApiKey(String openAiApiKey) { this.openAiApiKey = openAiApiKey; }
    public String getOpenAiModel() { return openAiModel; }
    public void setOpenAiModel(String openAiModel) { this.openAiModel = openAiModel; }
    public boolean isOllamaEnabled() { return ollamaEnabled; }
    public void setOllamaEnabled(boolean ollamaEnabled) { this.ollamaEnabled = ollamaEnabled; }
    public String getOllamaBaseUrl() { return ollamaBaseUrl; }
    public void setOllamaBaseUrl(String ollamaBaseUrl) { this.ollamaBaseUrl = ollamaBaseUrl; }
    public String getOllamaModel() { return ollamaModel; }
    public void setOllamaModel(String ollamaModel) { this.ollamaModel = ollamaModel; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
