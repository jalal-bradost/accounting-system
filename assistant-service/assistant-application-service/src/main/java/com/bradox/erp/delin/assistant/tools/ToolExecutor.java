package com.bradox.erp.assistant.tools;

import com.bradox.erp.assistant.config.AiProperties;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.platform.security.AuthorizationPort;
import com.bradox.erp.platform.security.ForbiddenException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ToolRegistry registry;
    private final AuthorizationPort authorizationPort;
    private final AuditLogPort auditLogPort;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    public ToolExecutor(ToolRegistry registry,
                        AuthorizationPort authorizationPort,
                        AuditLogPort auditLogPort,
                        ObjectMapper objectMapper,
                        AiProperties aiProperties) {
        this.registry = registry;
        this.authorizationPort = authorizationPort;
        this.auditLogPort = auditLogPort;
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
    }

    public ToolResult execute(String toolName,
                              String argumentsJson,
                              CompanyId companyId,
                              UserId userId,
                              UUID conversationId,
                              String currencyCode) {
        long started = System.currentTimeMillis();
        ErpTool tool = registry.find(toolName).orElse(null);
        if (tool == null) {
            audit(companyId, conversationId, toolName, false, "unknown_tool", started);
            return ToolResult.error("Unknown tool: " + toolName);
        }
        Set<String> needed = tool.requiredPermissions();
        if (userId == null || !authorizationPort.hasAny(userId, needed)) {
            audit(companyId, conversationId, toolName, false, "permission_denied", started);
            String neededLabel = needed.size() == 1
                    ? needed.iterator().next()
                    : String.join(" or ", needed);
            throw new ForbiddenException(
                    "error.security.forbidden",
                    new Object[]{neededLabel},
                    "Missing required permission: " + neededLabel);
        }
        try {
            JsonNode args = objectMapper.readTree(argumentsJson != null && !argumentsJson.isBlank() ? argumentsJson : "{}");
            ErpTool.ToolContext ctx = new ErpTool.ToolContext(companyId, userId, conversationId, currencyCode);
            ToolResult result = tool.execute(ctx, args);
            audit(companyId, conversationId, toolName, result.isSuccess(),
                    result.isSuccess() ? "ok" : "tool_error", started);
            return result;
        } catch (ForbiddenException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            audit(companyId, conversationId, toolName, false, "invalid_args", started);
            return ToolResult.error(ex.getMessage());
        } catch (Exception ex) {
            log.warn("Tool {} failed: {}", toolName, ex.getMessage());
            audit(companyId, conversationId, toolName, false, "exception", started);
            return ToolResult.error("Tool execution failed. Please try again.");
        }
    }

    private void audit(CompanyId companyId, UUID conversationId, String toolName, boolean success, String status, long started) {
        if (companyId == null || !aiProperties.isEnabled()) {
            return;
        }
        try {
            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("toolName", toolName);
            changes.put("success", success);
            changes.put("status", status);
            changes.put("durationMs", System.currentTimeMillis() - started);
            auditLogPort.recordBusinessEvent(
                    companyId,
                    "assistant.tool",
                    conversationId != null ? conversationId : companyId.getId(),
                    "AI tool " + toolName + " (" + status + ")",
                    changes);
        } catch (Exception ex) {
            log.debug("Failed to audit AI tool call: {}", ex.getMessage());
        }
    }
}
