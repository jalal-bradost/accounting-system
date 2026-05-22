package com.jalaldeveloper.accountingsystem.platform.settings;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RoleEntity;

import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        UUID companyId,
        String code,
        String name,
        String description,
        boolean systemRole,
        boolean active,
        int userCount,
        List<UUID> permissionIds,
        List<String> permissionCodes) {

    public static RoleResponse of(RoleEntity r,
                                  boolean systemRole,
                                  int userCount,
                                  List<UUID> permissionIds,
                                  List<String> permissionCodes) {
        return new RoleResponse(
                r.getId(), r.getCompanyId(), r.getCode(), r.getName(), r.getDescription(),
                systemRole, r.isActive(), userCount,
                permissionIds == null ? List.of() : permissionIds,
                permissionCodes == null ? List.of() : permissionCodes);
    }
}
