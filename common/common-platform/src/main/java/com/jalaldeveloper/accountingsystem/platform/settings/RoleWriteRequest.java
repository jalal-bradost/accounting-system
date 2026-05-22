package com.jalaldeveloper.accountingsystem.platform.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record RoleWriteRequest(
        @NotBlank @Size(min = 2, max = 50)
        @Pattern(regexp = "[A-Za-z0-9_\\-]+", message = "Code must be alphanumeric, underscore or dash")
        String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        Set<UUID> permissionIds) {}
