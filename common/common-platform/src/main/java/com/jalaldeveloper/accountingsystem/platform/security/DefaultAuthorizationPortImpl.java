package com.jalaldeveloper.accountingsystem.platform.security;

import com.jalaldeveloper.accountingsystem.domain.valueobject.UserId;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.PermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RolePermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.UserRoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.PermissionJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RolePermissionJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.UserRoleJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link AuthorizationPort}. Permissive when no user is
 * present on the request (current dev behavior — preserves backwards compatibility
 * with controllers that have no authentication wired). When a user is present the
 * port resolves the user's effective permission codes from the RBAC tables and
 * checks them against the required set.
 *
 * <p>Replace this bean with a Spring Security backed one when authentication lands.
 */
@Component
public class DefaultAuthorizationPortImpl implements AuthorizationPort {

    private final UserRoleJpaRepository userRoleRepository;
    private final RolePermissionJpaRepository rolePermissionRepository;
    private final PermissionJpaRepository permissionRepository;

    public DefaultAuthorizationPortImpl(UserRoleJpaRepository userRoleRepository,
                                        RolePermissionJpaRepository rolePermissionRepository,
                                        PermissionJpaRepository permissionRepository) {
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public boolean hasAll(UserId userId, Set<String> requiredPermissions) {
        if (userId == null) return true;
        Set<String> effective = effectivePermissions(userId);
        return effective.containsAll(requiredPermissions);
    }

    @Override
    public boolean hasAny(UserId userId, Set<String> requiredPermissions) {
        if (userId == null) return true;
        Set<String> effective = effectivePermissions(userId);
        return requiredPermissions.stream().anyMatch(effective::contains);
    }

    private Set<String> effectivePermissions(UserId userId) {
        List<UUID> roleIds = userRoleRepository.findByUserId(userId.getId()).stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) return Set.of();

        List<UUID> permissionIds = rolePermissionRepository.findByRoleIdIn(roleIds).stream()
                .map(RolePermissionEntity::getPermissionId)
                .distinct()
                .collect(Collectors.toList());
        if (permissionIds.isEmpty()) return Set.of();

        return permissionRepository.findAllById(permissionIds).stream()
                .map(PermissionEntity::getCode)
                .collect(Collectors.toSet());
    }
}
