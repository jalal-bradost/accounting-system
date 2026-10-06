package com.bradox.erp.assistant.settings;

import com.bradox.erp.assistant.config.AiProperties;
import com.bradox.erp.assistant.dataaccess.entity.AssistantSettingsEntity;
import com.bradox.erp.assistant.dataaccess.repository.AssistantSettingsJpaRepository;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AssistantSettingsService {

    private static final Set<String> MODES = Set.of("hybrid", "google", "openai", "ollama");

    private final AssistantSettingsJpaRepository repository;
    private final AiProperties defaults;

    public AssistantSettingsService(AssistantSettingsJpaRepository repository, AiProperties defaults) {
        this.repository = repository;
        this.defaults = defaults;
    }

    public AiRuntimeSettings resolve(CompanyId companyId) {
        UUID id = companyId.getId();
        return repository.findById(id)
                .map(this::toRuntime)
                .orElseGet(this::defaultsRuntime);
    }

    public AssistantSettingsResponse getResponse(CompanyId companyId) {
        AiRuntimeSettings runtime = resolve(companyId);
        return toResponse(runtime);
    }

    @Transactional
    public AssistantSettingsResponse update(CompanyId companyId, AssistantSettingsUpdateRequest request) {
        UUID id = companyId.getId();
        AssistantSettingsEntity entity = repository.findById(id).orElseGet(() -> {
            AssistantSettingsEntity created = defaultsEntity(id);
            return created;
        });

        if (request.getAssistantEnabled() != null) {
            entity.setAssistantEnabled(request.getAssistantEnabled());
        }
        if (request.getMode() != null && !request.getMode().isBlank()) {
            String mode = request.getMode().trim().toLowerCase(Locale.ROOT);
            if (!MODES.contains(mode)) {
                throw new IllegalArgumentException("mode must be hybrid, google, openai, or ollama");
            }
            entity.setMode(mode);
        }
        if (request.getGoogleEnabled() != null) {
            entity.setGoogleEnabled(request.getGoogleEnabled());
        }
        if (Boolean.TRUE.equals(request.getClearGoogleApiKey())) {
            entity.setGoogleApiKey(null);
        } else if (request.getGoogleApiKey() != null && !request.getGoogleApiKey().isBlank()) {
            entity.setGoogleApiKey(request.getGoogleApiKey().trim());
        }
        if (request.getGoogleModel() != null && !request.getGoogleModel().isBlank()) {
            entity.setGoogleModel(request.getGoogleModel().trim());
        }
        if (request.getOpenAiEnabled() != null) {
            entity.setOpenAiEnabled(request.getOpenAiEnabled());
        }
        if (Boolean.TRUE.equals(request.getClearOpenAiApiKey())) {
            entity.setOpenAiApiKey(null);
        } else if (request.getOpenAiApiKey() != null && !request.getOpenAiApiKey().isBlank()) {
            entity.setOpenAiApiKey(request.getOpenAiApiKey().trim());
        }
        if (request.getOpenAiModel() != null && !request.getOpenAiModel().isBlank()) {
            entity.setOpenAiModel(request.getOpenAiModel().trim());
        }
        if (request.getOllamaEnabled() != null) {
            entity.setOllamaEnabled(request.getOllamaEnabled());
        }
        if (request.getOllamaBaseUrl() != null && !request.getOllamaBaseUrl().isBlank()) {
            entity.setOllamaBaseUrl(trimSlash(request.getOllamaBaseUrl().trim()));
        }
        if (request.getOllamaModel() != null && !request.getOllamaModel().isBlank()) {
            entity.setOllamaModel(request.getOllamaModel().trim());
        }
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
        return toResponse(toRuntime(entity));
    }

    private AssistantSettingsEntity defaultsEntity(UUID companyId) {
        AssistantSettingsEntity e = new AssistantSettingsEntity();
        e.setCompanyId(companyId);
        e.setAssistantEnabled(defaults.isEnabled());
        e.setMode(normalizeMode(defaults.getProvider()));
        e.setGoogleEnabled(defaults.getGoogle().isEnabled());
        e.setGoogleApiKey(blankToNull(defaults.getGoogle().getApiKey()));
        e.setGoogleModel(defaults.getGoogle().getModel());
        e.setOpenAiEnabled(defaults.getOpenai().isEnabled());
        e.setOpenAiApiKey(blankToNull(defaults.getOpenai().getApiKey()));
        e.setOpenAiModel(defaults.getOpenai().getModel());
        e.setOllamaEnabled(defaults.getOllama().isEnabled());
        e.setOllamaBaseUrl(defaults.getOllama().getBaseUrl());
        e.setOllamaModel(defaults.getOllama().getModel());
        e.setUpdatedAt(Instant.now());
        return e;
    }

    private AiRuntimeSettings defaultsRuntime() {
        return new AiRuntimeSettings(
                defaults.isEnabled(),
                normalizeMode(defaults.getProvider()),
                defaults.getGoogle().isEnabled(),
                blankToNull(defaults.getGoogle().getApiKey()),
                defaults.getGoogle().getModel(),
                defaults.getOpenai().isEnabled(),
                blankToNull(defaults.getOpenai().getApiKey()),
                defaults.getOpenai().getModel(),
                defaults.getOllama().isEnabled(),
                defaults.getOllama().getBaseUrl(),
                defaults.getOllama().getModel());
    }

    private AiRuntimeSettings toRuntime(AssistantSettingsEntity e) {
        return new AiRuntimeSettings(
                e.isAssistantEnabled(),
                normalizeMode(e.getMode()),
                e.isGoogleEnabled(),
                blankToNull(e.getGoogleApiKey()),
                e.getGoogleModel() != null ? e.getGoogleModel() : defaults.getGoogle().getModel(),
                e.isOpenAiEnabled(),
                blankToNull(e.getOpenAiApiKey()),
                e.getOpenAiModel() != null ? e.getOpenAiModel() : defaults.getOpenai().getModel(),
                e.isOllamaEnabled(),
                e.getOllamaBaseUrl() != null ? e.getOllamaBaseUrl() : defaults.getOllama().getBaseUrl(),
                e.getOllamaModel() != null ? e.getOllamaModel() : defaults.getOllama().getModel());
    }

    private AssistantSettingsResponse toResponse(AiRuntimeSettings runtime) {
        String googleKey = runtime.googleApiKey();
        boolean googleSet = googleKey != null && !googleKey.isBlank();
        String openAiKey = runtime.openAiApiKey();
        boolean openAiSet = openAiKey != null && !openAiKey.isBlank();
        return new AssistantSettingsResponse(
                runtime.assistantEnabled(),
                runtime.mode(),
                runtime.googleEnabled(),
                googleSet,
                maskKey(googleKey),
                runtime.googleModel(),
                runtime.openAiEnabled(),
                openAiSet,
                maskKey(openAiKey),
                runtime.openAiModel(),
                runtime.ollamaEnabled(),
                runtime.ollamaBaseUrl(),
                runtime.ollamaModel());
    }

    private static String normalizeMode(String providerOrMode) {
        if (providerOrMode == null || providerOrMode.isBlank()) {
            return "hybrid";
        }
        String v = providerOrMode.trim().toLowerCase(Locale.ROOT);
        if ("google".equals(v) || "gemini".equals(v) || "google-ai-studio".equals(v)) {
            return "google";
        }
        if ("openai".equals(v) || "gpt".equals(v)) {
            return "openai";
        }
        if ("ollama".equals(v) || "local".equals(v)) {
            return "ollama";
        }
        return "hybrid";
    }

    private static String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.length() <= 4) {
            return "****";
        }
        return "••••" + key.substring(key.length() - 4);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
