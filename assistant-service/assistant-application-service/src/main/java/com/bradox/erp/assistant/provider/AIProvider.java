package com.bradox.erp.assistant.provider;

import com.bradox.erp.assistant.settings.AiRuntimeSettings;

public interface AIProvider {

    String id();

    AIGenerationResult generate(AIGenerationRequest request, AiRuntimeSettings settings);

    boolean isAvailable(AiRuntimeSettings settings);
}
