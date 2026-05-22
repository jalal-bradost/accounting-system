package com.jalaldeveloper.accountingsystem.platform.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Email @Size(max = 200) String email,
        @Size(max = 200) String displayName,
        @NotBlank @Size(min = 8, max = 200) String password,
        Set<UUID> roleIds) {}
