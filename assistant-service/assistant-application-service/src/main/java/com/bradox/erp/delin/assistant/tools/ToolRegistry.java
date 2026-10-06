package com.bradox.erp.assistant.tools;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ToolRegistry {

    private final Map<String, ErpTool> toolsByName = new LinkedHashMap<>();

    public ToolRegistry(List<ErpTool> tools) {
        if (tools != null) {
            for (ErpTool tool : tools) {
                toolsByName.put(tool.name(), tool);
            }
        }
    }

    public Optional<ErpTool> find(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    public Collection<ErpTool> all() {
        return List.copyOf(toolsByName.values());
    }
}
