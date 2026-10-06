package com.bradox.erp.assistant.provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AIGenerationRequest {

    private final List<Message> messages;
    private final List<ToolDefinition> tools;

    public AIGenerationRequest(List<Message> messages, List<ToolDefinition> tools) {
        this.messages = messages != null ? List.copyOf(messages) : List.of();
        this.tools = tools != null ? List.copyOf(tools) : List.of();
    }

    public List<Message> getMessages() {
        return messages;
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public record Message(String role, String content, List<ToolCall> toolCalls, String toolCallId, String name) {
        public static Message system(String content) {
            return new Message("system", content, null, null, null);
        }

        public static Message user(String content) {
            return new Message("user", content, null, null, null);
        }

        public static Message assistant(String content, List<ToolCall> toolCalls) {
            return new Message("assistant", content, toolCalls, null, null);
        }

        public static Message toolResult(String toolCallId, String name, String content) {
            return new Message("tool", content, null, toolCallId, name);
        }
    }

    public record ToolCall(String id, String name, String argumentsJson, String thoughtSignature) {
        public ToolCall(String id, String name, String argumentsJson) {
            this(id, name, argumentsJson, null);
        }
    }

    public record ToolDefinition(String name, String description, Map<String, Object> parametersSchema) {
        public Map<String, Object> toOllamaTool() {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", name);
            function.put("description", description);
            function.put("parameters", parametersSchema != null ? parametersSchema : Map.of("type", "object", "properties", Map.of()));
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("type", "function");
            tool.put("function", function);
            return tool;
        }
    }

    public static List<Message> mutableCopy(List<Message> source) {
        return new ArrayList<>(source);
    }
}
