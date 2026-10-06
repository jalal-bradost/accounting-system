package com.bradox.erp.assistant.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured tool output returned to the model and optionally surfaced to the UI.
 */
public final class ToolResult {

    private final boolean success;
    private final String errorMessage;
    private final Map<String, Object> data;
    private final List<Map<String, Object>> artifacts;

    private ToolResult(boolean success, String errorMessage, Map<String, Object> data, List<Map<String, Object>> artifacts) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.data = data != null ? Map.copyOf(data) : Map.of();
        this.artifacts = artifacts != null ? List.copyOf(artifacts) : List.of();
    }

    public static ToolResult ok(Map<String, Object> data) {
        return new ToolResult(true, null, data, List.of());
    }

    public static ToolResult ok(Map<String, Object> data, List<Map<String, Object>> artifacts) {
        return new ToolResult(true, null, data, artifacts);
    }

    public static ToolResult error(String message) {
        return new ToolResult(false, message, Map.of("error", message), List.of());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public List<Map<String, Object>> getArtifacts() {
        return artifacts;
    }

    public Map<String, Object> toModelPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", success);
        if (!success) {
            payload.put("error", errorMessage);
        }
        payload.putAll(data);
        return payload;
    }

    public static Map<String, Object> kpiArtifact(String label, Object value, String currency) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("type", "kpi");
        a.put("label", label);
        a.put("value", value);
        if (currency != null) {
            a.put("currency", currency);
        }
        return a;
    }

    public static Map<String, Object> tableArtifact(String label, List<String> columns, List<List<Object>> rows) {
        return tableArtifact(label, columns, rows, null);
    }

    public static Map<String, Object> tableArtifact(String label, List<String> columns, List<List<Object>> rows, Long total) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("type", "table");
        a.put("label", label);
        a.put("columns", columns);
        a.put("rows", rows != null ? rows : new ArrayList<>());
        if (total != null) {
            a.put("total", total);
        }
        return a;
    }
}
