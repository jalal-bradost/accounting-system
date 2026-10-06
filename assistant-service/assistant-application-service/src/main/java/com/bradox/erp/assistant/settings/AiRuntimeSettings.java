package com.bradox.erp.assistant.settings;

/**
 * Effective runtime settings for one company (DB row merged over YAML defaults).
 */
public record AiRuntimeSettings(
        boolean assistantEnabled,
        String mode,
        boolean googleEnabled,
        String googleApiKey,
        String googleModel,
        boolean openAiEnabled,
        String openAiApiKey,
        String openAiModel,
        boolean ollamaEnabled,
        String ollamaBaseUrl,
        String ollamaModel
) {
    public boolean useGoogle() {
        return googleEnabled && googleApiKey != null && !googleApiKey.isBlank();
    }

    public boolean useOpenAi() {
        return openAiEnabled && openAiApiKey != null && !openAiApiKey.isBlank()
                && openAiModel != null && !openAiModel.isBlank();
    }

    public boolean useOllama() {
        return ollamaEnabled && ollamaBaseUrl != null && !ollamaBaseUrl.isBlank()
                && ollamaModel != null && !ollamaModel.isBlank();
    }
}
