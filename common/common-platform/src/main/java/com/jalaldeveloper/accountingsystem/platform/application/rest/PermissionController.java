package com.jalaldeveloper.accountingsystem.platform.application.rest;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.PermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.PermissionJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/platform/permissions", produces = "application/json")
public class PermissionController {

    private static final List<String> MODULE_ORDER = List.of(
            "platform", "accounting", "contacts", "inventory", "purchase", "sales");

    private final PermissionJpaRepository repository;

    public PermissionController(PermissionJpaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @RequiresPermission("platform.permission.read")
    public ResponseEntity<PermissionCatalogResponse> list() {
        List<PermissionEntity> permissions = repository.findAll();
        permissions.sort(Comparator.comparing(PermissionEntity::getCode));

        Map<String, List<PermissionDto>> grouped = new LinkedHashMap<>();
        // Pre-seed groups in canonical order so the UI renders modules in a stable layout.
        for (String m : MODULE_ORDER) {
            grouped.put(m, new ArrayList<>());
        }
        for (PermissionEntity p : permissions) {
            String module = moduleOf(p.getCode());
            grouped.computeIfAbsent(module, k -> new ArrayList<>())
                    .add(new PermissionDto(p.getId(), p.getCode(), p.getDescription(), module, actionOf(p.getCode())));
        }

        List<ModuleGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<PermissionDto>> e : grouped.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            groups.add(new ModuleGroup(e.getKey(), e.getValue()));
        }
        return ResponseEntity.ok(new PermissionCatalogResponse(permissions.size(), groups));
    }

    private static String moduleOf(String code) {
        int dot = code.indexOf('.');
        return dot < 0 ? "other" : code.substring(0, dot);
    }

    private static String actionOf(String code) {
        int last = code.lastIndexOf('.');
        return last < 0 ? code : code.substring(last + 1);
    }

    public record PermissionDto(UUID id, String code, String description, String module, String action) {}

    public record ModuleGroup(String module, List<PermissionDto> permissions) {}

    public record PermissionCatalogResponse(int total, List<ModuleGroup> groups) {}
}
