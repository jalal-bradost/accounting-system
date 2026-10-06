package com.bradox.erp.assistant.provider;

import java.util.List;

public final class AIGenerationResult {

    private final String content;
    private final List<AIGenerationRequest.ToolCall> toolCalls;
    private final boolean done;

    public AIGenerationResult(String content, List<AIGenerationRequest.ToolCall> toolCalls, boolean done) {
        this.content = content;
        this.toolCalls = toolCalls != null ? List.copyOf(toolCalls) : List.of();
        this.done = done;
    }

    public String getContent() {
        return content;
    }

    public List<AIGenerationRequest.ToolCall> getToolCalls() {
        return toolCalls;
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    public boolean isDone() {
        return done;
    }
}
