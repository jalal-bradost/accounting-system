package com.bradox.erp.assistant.tools.support;

import com.bradox.erp.assistant.dates.RelativeDateResolver;
import com.bradox.erp.assistant.tools.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ToolSchemas {

    private ToolSchemas() {}

    public static Map<String, Object> periodOrRangeSchema(boolean includeLimit) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("period", Map.of(
                "type", "string",
                "description", "Relative period (default THIS_MONTH if omitted): TODAY, YESTERDAY, THIS_WEEK, LAST_WEEK, THIS_MONTH, LAST_MONTH, THIS_QUARTER, LAST_QUARTER, THIS_YEAR, LAST_YEAR, LAST_30_DAYS, LAST_90_DAYS"));
        properties.put("from", Map.of("type", "string", "description", "ISO date YYYY-MM-DD (optional if period set)"));
        properties.put("to", Map.of("type", "string", "description", "ISO date YYYY-MM-DD (optional if period set)"));
        if (includeLimit) {
            properties.put("limit", Map.of("type", "integer", "description", "Max rows to return (default 10, max 25)"));
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    public static RelativeDateResolver.DateRange resolveDates(RelativeDateResolver resolver, JsonNode args) {
        String period = text(args, "period");
        LocalDate from = date(args, "from");
        LocalDate to = date(args, "to");
        return resolver.resolve(period, from, to);
    }

    public static String text(JsonNode args, String field) {
        if (args == null || !args.has(field) || args.get(field).isNull()) {
            return null;
        }
        String v = args.get(field).asText(null);
        return v != null && !v.isBlank() ? v.trim() : null;
    }

    public static LocalDate date(JsonNode args, String field) {
        String v = text(args, field);
        if (v == null) {
            return null;
        }
        return LocalDate.parse(v);
    }

    public static int limit(JsonNode args, int defaultValue, int max) {
        if (args == null || !args.has("limit") || args.get("limit").isNull()) {
            return defaultValue;
        }
        int v = args.get("limit").asInt(defaultValue);
        if (v < 1) {
            return defaultValue;
        }
        return Math.min(v, max);
    }

    public static Map<String, Object> baseFinancial(RelativeDateResolver.DateRange range, String currency) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from", range.from().toString());
        data.put("to", range.to().toString());
        data.put("currency", currency);
        return data;
    }

    public static List<Map<String, Object>> singleKpi(String label, Object value, String currency) {
        return List.of(ToolResult.kpiArtifact(label, value, currency));
    }
}
