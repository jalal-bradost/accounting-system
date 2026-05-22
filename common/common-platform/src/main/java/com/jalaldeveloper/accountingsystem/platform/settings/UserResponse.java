package com.jalaldeveloper.accountingsystem.platform.settings;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AppUserEntity;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID companyId,
        String username,
        String email,
        String displayName,
        boolean active,
        List<RoleSummary> roles) {

    public static UserResponse of(AppUserEntity u, List<RoleSummary> roles) {
        return new UserResponse(
                u.getId(),
                u.getCompanyId(),
                u.getUsername(),
                u.getEmail(),
                u.getDisplayName(),
                u.isActive(),
                roles == null ? List.of() : roles);
    }

    public record RoleSummary(UUID id, String code, String name) {}
}
