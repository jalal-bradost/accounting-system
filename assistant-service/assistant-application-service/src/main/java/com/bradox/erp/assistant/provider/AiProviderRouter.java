package com.bradox.erp.assistant.provider;

import com.bradox.erp.assistant.settings.AiRuntimeSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes to Google AI Studio, OpenAI, and/or Ollama based on company settings.
 * Hybrid mode tries Google, then OpenAI, then Ollama on failure.
 */
@Component
public class AiProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(AiProviderRouter.class);

    private final Map<String, AIProvider> providersById;

    public AiProviderRouter(List<AIProvider> providers) {
        this.providersById = providers.stream()
                .collect(Collectors.toMap(AIProvider::id, Function.identity(), (a, b) -> a));
    }

    public AIGenerationResult generate(AIGenerationRequest request, AiRuntimeSettings settings) {
        List<AIProvider> chain = buildChain(settings);
        if (chain.isEmpty()) {
            throw new AIProviderException(
                    "No AI provider is enabled. Configure Google AI Studio, OpenAI, and/or Ollama in Settings.");
        }
        AIProviderException last = null;
        for (int i = 0; i < chain.size(); i++) {
            AIProvider provider = chain.get(i);
            try {
                log.debug("Using AI provider {}", provider.id());
                long started = System.nanoTime();
                AIGenerationResult result = provider.generate(request, settings);
                log.info("AI provider {} succeeded in {}ms", provider.id(),
                        (System.nanoTime() - started) / 1_000_000L);
                return result;
            } catch (AIProviderException ex) {
                last = ex;
                boolean hasFallback = i < chain.size() - 1;
                if (hasFallback) {
                    log.warn("AI provider {} failed ({}), trying fallback", provider.id(), ex.getMessage());
                } else {
                    throw ex;
                }
            }
        }
        throw last != null ? last : new AIProviderException("AI providers failed.");
    }

    private List<AIProvider> buildChain(AiRuntimeSettings settings) {
        List<AIProvider> chain = new ArrayList<>();
        String mode = settings.mode() != null ? settings.mode() : "hybrid";
        switch (mode) {
            case "google" -> addIf(chain, "google", settings.useGoogle());
            case "openai" -> addIf(chain, "openai", settings.useOpenAi());
            case "ollama" -> addIf(chain, "ollama", settings.useOllama());
            default -> {
                // hybrid: Google → OpenAI → Ollama
                addIf(chain, "google", settings.useGoogle());
                addIf(chain, "openai", settings.useOpenAi());
                addIf(chain, "ollama", settings.useOllama());
            }
        }
        return chain;
    }

    private void addIf(List<AIProvider> chain, String id, boolean enabled) {
        if (!enabled) {
            return;
        }
        AIProvider provider = providersById.get(id);
        if (provider != null) {
            chain.add(provider);
        }
    }
}
