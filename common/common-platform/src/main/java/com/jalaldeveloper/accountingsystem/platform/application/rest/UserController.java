package com.jalaldeveloper.accountingsystem.platform.application.rest;

import com.jalaldeveloper.accountingsystem.platform.application.dto.PageResponse;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.settings.CreateUserRequest;
import com.jalaldeveloper.accountingsystem.platform.settings.UpdateUserRequest;
import com.jalaldeveloper.accountingsystem.platform.settings.UserApplicationService;
import com.jalaldeveloper.accountingsystem.platform.settings.UserResponse;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/platform/users", produces = "application/json")
public class UserController {

    private final UserApplicationService service;
    private final CompanyContext companyContext;

    public UserController(UserApplicationService service, CompanyContext companyContext) {
        this.service = service;
        this.companyContext = companyContext;
    }

    @GetMapping
    @RequiresPermission("platform.user.read")
    public ResponseEntity<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        UUID companyId = companyContext.requireCompany().getId();
        var pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        var result = service.list(companyId, q, active, pageable);
        return ResponseEntity.ok(PageResponse.of(result, Function.identity()));
    }

    @GetMapping("/{id}")
    @RequiresPermission("platform.user.read")
    public ResponseEntity<UserResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    @RequiresPermission("platform.user.write")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest body) {
        UUID companyId = companyContext.requireCompany().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(companyId, body));
    }

    @PutMapping("/{id}")
    @RequiresPermission("platform.user.write")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateUserRequest body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @PutMapping("/{id}/roles")
    @RequiresPermission("platform.user.write")
    public ResponseEntity<UserResponse> setRoles(@PathVariable UUID id,
                                                 @Valid @RequestBody SetRolesRequest body) {
        return ResponseEntity.ok(service.setRoles(id, body.roleIds() == null ? Set.of() : body.roleIds()));
    }

    @PostMapping("/{id}/reset-password")
    @RequiresPermission("platform.user.write")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id,
                                              @Valid @RequestBody ResetPasswordRequest body) {
        service.resetPassword(id, body.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @RequiresPermission("platform.user.write")
    public ResponseEntity<UserResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.setActive(id, true));
    }

    @PostMapping("/{id}/deactivate")
    @RequiresPermission("platform.user.write")
    public ResponseEntity<UserResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.setActive(id, false));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("platform.user.write")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record SetRolesRequest(Set<UUID> roleIds) {}

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 200) String newPassword) {}
}
