package com.bradox.erp.assistant.tools;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ErpTool {

    String name();

    String description();

    /** JSON Schema object for tool parameters (Ollama/OpenAI style). */
    Map<String, Object> inputSchema();

    /** Primary domain permission required in addition to platform.assistant.use. */
    String requiredPermission();

    /**
     * Any-of gate: the tool is exposed/executable when the user has at least one of these.
     * Defaults to {@code Set.of(requiredPermission())}. Multi-entity tools override this.
     */
    default Set<String> requiredPermissions() {
        return Set.of(requiredPermission());
    }

    ToolResult execute(ToolContext context, JsonNode arguments);

    record ToolContext(
            CompanyId companyId,
            UserId userId,
            UUID conversationId,
            String currencyCode
    ) {}
}
