package com.bradox.erp.assistant.settings;

public record AssistantSettingsResponse(
        boolean assistantEnabled,
        String mode,
        boolean googleEnabled,
        boolean googleApiKeySet,
        String googleApiKeyMasked,
        String googleModel,
        boolean openAiEnabled,
        boolean openAiApiKeySet,
        String openAiApiKeyMasked,
        String openAiModel,
        boolean ollamaEnabled,
        String ollamaBaseUrl,
        String ollamaModel
) {}
