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

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local Ollama chat API with tool calling.
 */
@Component
public class OllamaAIProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaAIProvider.class);

    private final AiProperties defaults;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, RestClient> clients = new ConcurrentHashMap<>();

    public OllamaAIProvider(AiProperties defaults, ObjectMapper objectMapper) {
        this.defaults = defaults;
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return "ollama";
    }

    @Override
    public AIGenerationResult generate(AIGenerationRequest request, AiRuntimeSettings settings) {
        if (!settings.useOllama()) {
            throw new AIProviderException("Ollama provider is disabled or not configured.");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", settings.ollamaModel());
            body.put("stream", false);
            // Qwen3 thinking models otherwise spend a long time on hidden reasoning tokens.
            body.put("think", false);
            // Keep the model loaded on the dedicated GPU host between requests.
            body.put("keep_alive", -1);
            body.put("messages", toOllamaMessages(request.getMessages()));
            if (!request.getTools().isEmpty()) {
                body.put("tools", request.getTools().stream().map(AIGenerationRequest.ToolDefinition::toOllamaTool).toList());
            }

            long started = System.nanoTime();
            String raw = clientFor(settings.ollamaBaseUrl()).post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            long wallMs = (System.nanoTime() - started) / 1_000_000L;

            JsonNode response = objectMapper.readTree(raw != null ? raw : "{}");
            if (!response.has("message")) {
                throw new AIProviderException("Ollama returned an empty chat response");
            }
            logOllamaTiming(settings.ollamaModel(), wallMs, response);
            return parseMessage(response.get("message"), response.path("done").asBoolean(true));
        } catch (AIProviderException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.warn("Ollama chat request failed: {}", ex.getMessage());
            throw new AIProviderException("The local AI service is temporarily unavailable.", ex);
        } catch (Exception ex) {
            log.warn("Ollama chat parsing failed: {}", ex.getMessage());
            throw new AIProviderException("The local AI service returned an unexpected response.", ex);
        }
    }

    private void logOllamaTiming(String model, long wallMs, JsonNode response) {
        long loadMs = response.path("load_duration").asLong(0L) / 1_000_000L;
        long promptMs = response.path("prompt_eval_duration").asLong(0L) / 1_000_000L;
        long evalMs = response.path("eval_duration").asLong(0L) / 1_000_000L;
        long totalMs = response.path("total_duration").asLong(0L) / 1_000_000L;
        int promptTokens = response.path("prompt_eval_count").asInt(0);
        int evalTokens = response.path("eval_count").asInt(0);
        double tps = evalMs > 0 ? (evalTokens * 1000.0) / evalMs : 0.0;
        log.info("Ollama model={} wall={}ms load={}ms prompt={}ms/{}tok eval={}ms/{}tok ({}/s) total={}ms",
                model, wallMs, loadMs, promptMs, promptTokens, evalMs, evalTokens,
                String.format(java.util.Locale.ROOT, "%.1f", tps), totalMs);
    }

    @Override
    public boolean isAvailable(AiRuntimeSettings settings) {
        if (!settings.useOllama()) {
            return false;
        }
        try {
            String raw = clientFor(settings.ollamaBaseUrl()).get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(String.class);
            JsonNode tags = objectMapper.readTree(raw != null ? raw : "{}");
            if (!tags.has("models")) {
                return false;
            }
            String expected = settings.ollamaModel();
            String expectedBase = expected.contains(":") ? expected.substring(0, expected.indexOf(':')) : expected;
            for (JsonNode model : tags.get("models")) {
                String name = model.path("name").asText("");
                if (name.equals(expected) || name.startsWith(expectedBase + ":") || name.equals(expectedBase)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            log.debug("Ollama health check failed: {}", ex.getMessage());
            return false;
        }
    }

    private RestClient clientFor(String baseUrl) {
        String key = trimTrailingSlash(baseUrl);
        return clients.computeIfAbsent(key, url -> {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(defaults.getOllama().getConnectTimeoutMs()))
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofMillis(defaults.getOllama().getReadTimeoutMs()));
            return RestClient.builder().baseUrl(url).requestFactory(requestFactory).build();
        });
    }

    private AIGenerationResult parseMessage(JsonNode message, boolean done) {
        String content = message.path("content").asText(null);
        content = stripThinking(content);
        List<AIGenerationRequest.ToolCall> toolCalls = new ArrayList<>();
        JsonNode toolCallsNode = message.get("tool_calls");
        if (toolCallsNode != null && toolCallsNode.isArray()) {
            for (JsonNode tc : toolCallsNode) {
                JsonNode function = tc.has("function") ? tc.get("function") : tc;
                String name = function.path("name").asText(null);
                if (name == null || name.isBlank()) {
                    continue;
                }
                String id = tc.path("id").asText(null);
                if (id == null || id.isBlank()) {
                    id = "call_" + UUID.randomUUID();
                }
                String argsJson;
                JsonNode argsNode = function.get("arguments");
                if (argsNode == null || argsNode.isNull()) {
                    argsJson = "{}";
                } else if (argsNode.isTextual()) {
                    argsJson = argsNode.asText();
                } else {
                    argsJson = argsNode.toString();
                }
                toolCalls.add(new AIGenerationRequest.ToolCall(id, name, argsJson));
            }
        }
        return new AIGenerationResult(content, toolCalls, done);
    }


    /**
     * Reasoning models sometimes put chain-of-thought in content even with think:false.
     */
    static String stripThinking(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String cleaned = content;
        String openA = "<" + "think>";
        String closeA = "</" + "think>";
        String openB = "<" + "think>";
        String closeB = "</" + "think>";
        // Prefer explicit block removal for known open/close pairs.
        cleaned = removeBlocks(cleaned, openA, closeA);
        cleaned = removeBlocks(cleaned, openB, closeB);
        // Orphan closing tag: keep only what follows the last closer.
        int cut = -1;
        int closeLen = 0;
        for (String closer : new String[] { closeA, closeB }) {
            int idx = indexOfIgnoreCase(cleaned, closer);
            while (idx >= 0) {
                cut = idx;
                closeLen = closer.length();
                idx = indexOfIgnoreCase(cleaned, closer, idx + closer.length());
            }
        }
        if (cut >= 0) {
            cleaned = cleaned.substring(cut + closeLen);
        }
        return cleaned.trim();
    }

    private static String removeBlocks(String text, String open, String close) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int start = indexOfIgnoreCase(text, open, i);
            if (start < 0) {
                out.append(text.substring(i));
                break;
            }
            out.append(text, i, start);
            int end = indexOfIgnoreCase(text, close, start + open.length());
            if (end < 0) {
                // Unclosed block — drop the rest.
                return out.toString();
            }
            i = end + close.length();
        }
        return out.toString();
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        return indexOfIgnoreCase(haystack, needle, 0);
    }

    private static int indexOfIgnoreCase(String haystack, String needle, int from) {
        if (haystack == null || needle == null || from >= haystack.length()) {
            return -1;
        }
        return haystack.toLowerCase().indexOf(needle.toLowerCase(), Math.max(0, from));
    }

    private List<Map<String, Object>> toOllamaMessages(List<AIGenerationRequest.Message> messages) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (AIGenerationRequest.Message message : messages) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("role", message.role());
            row.put("content", message.content() != null ? message.content() : "");
            if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
                List<Map<String, Object>> calls = new ArrayList<>();
                for (AIGenerationRequest.ToolCall call : message.toolCalls()) {
                    Map<String, Object> fn = new LinkedHashMap<>();
                    fn.put("name", call.name());
                    try {
                        fn.put("arguments", objectMapper.readTree(call.argumentsJson() != null ? call.argumentsJson() : "{}"));
                    } catch (Exception ex) {
                        fn.put("arguments", Map.of());
                    }
                    Map<String, Object> tc = new LinkedHashMap<>();
                    tc.put("id", call.id());
                    tc.put("type", "function");
                    tc.put("function", fn);
                    calls.add(tc);
                }
                row.put("tool_calls", calls);
            }
            if (message.toolCallId() != null) {
                row.put("tool_call_id", message.toolCallId());
            }
            if (message.name() != null) {
                row.put("name", message.name());
            }
            out.add(row);
        }
        return out;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:11434";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
