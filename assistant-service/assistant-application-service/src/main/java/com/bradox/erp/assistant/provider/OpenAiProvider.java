package com.bradox.erp.assistant.provider;

import com.bradox.erp.assistant.config.AiProperties;
import com.bradox.erp.assistant.settings.AiRuntimeSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI Chat Completions API (GPT-4.1 / 4o / mini and compatible models).
 */
@Component
public class OpenAiProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);

    private static final int RETRY_MAX_ATTEMPTS = 3;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiProvider(AiProperties defaults, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        AiProperties.OpenAi openai = defaults.getOpenai();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(openai.getConnectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(openai.getReadTimeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(openai.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String id() {
        return "openai";
    }

    @Override
    public AIGenerationResult generate(AIGenerationRequest request, AiRuntimeSettings settings) {
        if (!settings.useOpenAi()) {
            throw new AIProviderException("OpenAI is disabled or API key is missing.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", settings.openAiModel());
        body.put("messages", toOpenAiMessages(request.getMessages()));
        if (!request.getTools().isEmpty()) {
            body.put("tools", request.getTools().stream().map(AIGenerationRequest.ToolDefinition::toOllamaTool).toList());
        }

        RestClientResponseException lastHttp = null;
        for (int attempt = 1; attempt <= RETRY_MAX_ATTEMPTS; attempt++) {
            try {
                String raw = restClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + settings.openAiApiKey())
                        .body(body)
                        .retrieve()
                        .body(String.class);

                JsonNode response = objectMapper.readTree(raw != null ? raw : "{}");
                JsonNode choices = response.get("choices");
                if (choices == null || !choices.isArray() || choices.isEmpty()) {
                    throw new AIProviderException("OpenAI returned an empty chat response");
                }
                return parseOpenAiMessage(choices.get(0).path("message"));
            } catch (AIProviderException ex) {
                throw ex;
            } catch (RestClientResponseException ex) {
                lastHttp = ex;
                int code = ex.getStatusCode().value();
                String detail = extractError(ex.getResponseBodyAsString());
                boolean retryable = code == 429 || code == 503 || code == 502;
                if (retryable && attempt < RETRY_MAX_ATTEMPTS) {
                    long sleepMs = 500L * (1L << (attempt - 1));
                    log.warn("OpenAI {} on {} (attempt {}/{}), retrying in {}ms: {}",
                            code, settings.openAiModel(), attempt, RETRY_MAX_ATTEMPTS, sleepMs,
                            detail != null ? detail : ex.getMessage());
                    sleepQuietly(sleepMs);
                    continue;
                }
                log.warn("OpenAI request failed ({}): {}", code, detail != null ? detail : ex.getMessage());
                throw new AIProviderException(
                        detail != null ? detail : "OpenAI request failed (" + code + ").",
                        ex);
            } catch (RestClientException ex) {
                log.warn("OpenAI request failed: {}", ex.getMessage());
                throw new AIProviderException("OpenAI is temporarily unavailable.", ex);
            } catch (Exception ex) {
                log.warn("OpenAI parsing failed: {}", ex.getMessage());
                throw new AIProviderException("OpenAI returned an unexpected response.", ex);
            }
        }
        String detail = lastHttp != null ? extractError(lastHttp.getResponseBodyAsString()) : null;
        throw new AIProviderException(detail != null ? detail : "OpenAI is temporarily unavailable.", lastHttp);
    }

    @Override
    public boolean isAvailable(AiRuntimeSettings settings) {
        return settings.useOpenAi();
    }

    private AIGenerationResult parseOpenAiMessage(JsonNode message) {
        String content = null;
        if (message.has("content") && !message.get("content").isNull()) {
            if (message.get("content").isTextual()) {
                content = message.get("content").asText();
            } else {
                content = message.get("content").toString();
            }
        }
        List<AIGenerationRequest.ToolCall> toolCalls = new ArrayList<>();
        JsonNode toolCallsNode = message.get("tool_calls");
        if (toolCallsNode != null && toolCallsNode.isArray()) {
            for (JsonNode tc : toolCallsNode) {
                JsonNode function = tc.path("function");
                String name = function.path("name").asText(null);
                if (name == null || name.isBlank()) {
                    continue;
                }
                String id = tc.path("id").asText("call_" + UUID.randomUUID());
                String argsJson = function.path("arguments").asText("{}");
                if (argsJson == null || argsJson.isBlank()) {
                    argsJson = "{}";
                }
                toolCalls.add(new AIGenerationRequest.ToolCall(id, name, argsJson));
            }
        }
        return new AIGenerationResult(content, toolCalls, true);
    }

    private List<Map<String, Object>> toOpenAiMessages(List<AIGenerationRequest.Message> messages) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (AIGenerationRequest.Message message : messages) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("role", message.role());
            if ("tool".equals(message.role())) {
                row.put("tool_call_id", message.toolCallId());
                row.put("content", message.content() != null ? message.content() : "");
            } else if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
                if (message.content() != null) {
                    row.put("content", message.content());
                }
                List<Map<String, Object>> calls = new ArrayList<>();
                for (AIGenerationRequest.ToolCall call : message.toolCalls()) {
                    Map<String, Object> fn = new LinkedHashMap<>();
                    fn.put("name", call.name());
                    fn.put("arguments", call.argumentsJson() != null ? call.argumentsJson() : "{}");
                    Map<String, Object> tc = new LinkedHashMap<>();
                    tc.put("id", call.id());
                    tc.put("type", "function");
                    tc.put("function", fn);
                    calls.add(tc);
                }
                row.put("tool_calls", calls);
            } else {
                row.put("content", message.content() != null ? message.content() : "");
            }
            out.add(row);
        }
        return out;
    }

    private String extractError(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode message = root.path("error").path("message");
            if (message.isTextual() && !message.asText().isBlank()) {
                String msg = message.asText().trim();
                if (msg.length() > 280) {
                    return msg.substring(0, 277) + "...";
                }
                return msg;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
