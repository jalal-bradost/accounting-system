package com.jalaldeveloper.accountingsystem.platform.application.rest;

import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.settings.RoleApplicationService;
import com.jalaldeveloper.accountingsystem.platform.settings.RoleResponse;
import com.jalaldeveloper.accountingsystem.platform.settings.RoleWriteRequest;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/platform/roles", produces = "application/json")
public class RoleController {

    private final RoleApplicationService service;
    private final CompanyContext companyContext;

    public RoleController(RoleApplicationService service, CompanyContext companyContext) {
        this.service = service;
        this.companyContext = companyContext;
    }

    @GetMapping
    @RequiresPermission("platform.role.read")
    public ResponseEntity<List<RoleResponse>> list() {
        UUID companyId = companyContext.requireCompany().getId();
        return ResponseEntity.ok(service.list(companyId));
    }

    @GetMapping("/{id}")
    @RequiresPermission("platform.role.read")
    public ResponseEntity<RoleResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    @RequiresPermission("platform.role.write")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleWriteRequest body) {
        UUID companyId = companyContext.requireCompany().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(companyId, body));
    }

    @PutMapping("/{id}")
    @RequiresPermission("platform.role.write")
    public ResponseEntity<RoleResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody RoleWriteRequest body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @PutMapping("/{id}/permissions")
    @RequiresPermission("platform.role.write")
    public ResponseEntity<RoleResponse> setPermissions(@PathVariable UUID id,
                                                       @RequestBody SetPermissionsRequest body) {
        return ResponseEntity.ok(service.setPermissions(id, body.permissionIds() == null ? Set.of() : body.permissionIds()));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("platform.role.write")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record SetPermissionsRequest(Set<UUID> permissionIds) {}
}
