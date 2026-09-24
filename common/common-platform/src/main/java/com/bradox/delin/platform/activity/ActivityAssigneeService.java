package com.bradox.delin.platform.activity;

import com.bradox.delin.domain.valueobject.UserId;
import com.bradox.delin.platform.dataaccess.entity.AppUserEntity;
import com.bradox.delin.platform.dataaccess.entity.PermissionEntity;
import com.bradox.delin.platform.dataaccess.entity.RoleEntity;
import com.bradox.delin.platform.dataaccess.entity.RolePermissionEntity;
import com.bradox.delin.platform.dataaccess.entity.UserRoleEntity;
import com.bradox.delin.platform.dataaccess.repository.AppUserJpaRepository;
import com.bradox.delin.platform.dataaccess.repository.PermissionJpaRepository;
import com.bradox.delin.platform.dataaccess.repository.RoleJpaRepository;
import com.bradox.delin.platform.dataaccess.repository.RolePermissionJpaRepository;
import com.bradox.delin.platform.dataaccess.repository.UserRoleJpaRepository;
import com.bradox.delin.platform.web.CompanyContext;
import com.bradox.delin.platform.web.UserDisplayNameService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityAssigneeService {

    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final AppUserJpaRepository userRepository;
    private final RoleJpaRepository roleRepository;
    private final UserRoleJpaRepository userRoleRepository;
    private final RolePermissionJpaRepository rolePermissionRepository;
    private final PermissionJpaRepository permissionRepository;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    public ActivityAssigneeService(AppUserJpaRepository userRepository,
                                   RoleJpaRepository roleRepository,
                                   UserRoleJpaRepository userRoleRepository,
                                   RolePermissionJpaRepository rolePermissionRepository,
                                   PermissionJpaRepository permissionRepository,
                                   ObjectProvider<CompanyContext> companyContextProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.companyContextProvider = companyContextProvider;
    }

    @Transactional(readOnly = true)
    public List<ActivityAssigneeResponse> listAssignees(UUID companyId, String modelName) {
        String permission = ActivityModelPermissions.requiredPermissionForModel(modelName);
        List<AppUserEntity> users = permission == null
                ? listActiveUsers(companyId)
                : listUsersWithPermission(companyId, permission);
        if (callerCannotAssignToAdmins(companyId)) {
            users = users.stream()
                    .filter(u -> !userHasAdminRole(u.getId(), companyId))
                    .toList();
        }
        return users.stream()
                .sorted(Comparator.comparing(ActivityAssigneeService::sortLabel, String.CASE_INSENSITIVE_ORDER))
                .map(ActivityAssigneeService::toResponse)
                .toList();
    }

    /**
     * Resolves the assignee lookup keys for inbox queries. Prefers an explicit {@code assigneeId}
     * (username or user UUID); otherwise uses the current request user. Returns both username and
     * UUID string when available so historical rows stored either way still match.
     */
    @Transactional(readOnly = true)
    public List<String> resolveAssigneeLookupKeys(UUID companyId, String assigneeId) {
        AppUserEntity user;
        if (assigneeId != null && !assigneeId.isBlank()) {
            user = findUser(companyId, assigneeId.trim())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Assignee not found in this company"));
        } else {
            CompanyContext ctx = companyContextProvider.getIfAvailable();
            if (ctx == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assigneeId required");
            }
            UserId current = ctx.currentUser()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Not authenticated"));
            user = userRepository.findById(current.getId())
                    .filter(u -> companyId.equals(u.getCompanyId()))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Current user not found in this company"));
        }
        java.util.LinkedHashSet<String> keys = new java.util.LinkedHashSet<>();
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            keys.add(user.getUsername());
        }
        if (user.getId() != null) {
            keys.add(user.getId().toString());
        }
        if (keys.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee has no lookup key");
        }
        return List.copyOf(keys);
    }

    /**
     * Ensures {@code assigneeId} (username or user UUID) refers to an active company user
     * who holds the model’s required permission when one is defined.
     * Non-admin callers cannot assign activities to users with the ADMIN role.
     */
    @Transactional(readOnly = true)
    public void validateAssignee(UUID companyId, String modelName, String assigneeId) {
        if (assigneeId == null || assigneeId.isBlank()) {
            return;
        }
        AppUserEntity user = findUser(companyId, assigneeId.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Assignee not found in this company"));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee is inactive");
        }
        if (callerCannotAssignToAdmins(companyId) && userHasAdminRole(user.getId(), companyId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Employees cannot assign activities to admins");
        }
        String permission = ActivityModelPermissions.requiredPermissionForModel(modelName);
        if (permission == null) {
            return;
        }
        if (!userHasPermission(user.getId(), companyId, permission)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Assignee does not have permission " + permission);
        }
    }

    /** True when the current caller is present and does not hold the company ADMIN role. */
    private boolean callerCannotAssignToAdmins(UUID companyId) {
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx == null) {
            return false;
        }
        Optional<UserId> current = ctx.currentUser();
        if (current.isEmpty()) {
            return false;
        }
        return !userHasAdminRole(current.get().getId(), companyId);
    }

    private boolean userHasAdminRole(UUID userId, UUID companyId) {
        Optional<RoleEntity> adminRole = roleRepository.findByCompanyIdAndCode(companyId, ADMIN_ROLE_CODE);
        if (adminRole.isEmpty() || !adminRole.get().isActive()) {
            return false;
        }
        UUID adminRoleId = adminRole.get().getId();
        return userRoleRepository.findByUserId(userId).stream()
                .anyMatch(ur -> adminRoleId.equals(ur.getRoleId()));
    }

    /** Prefer username as the stored assignee key (stable + matches existing clients). */
    public static String assigneeKey(AppUserEntity user) {
        return user.getUsername();
    }

    private List<AppUserEntity> listActiveUsers(UUID companyId) {
        return userRepository.search(companyId, null, true, PageRequest.of(0, 500)).getContent();
    }

    private List<AppUserEntity> listUsersWithPermission(UUID companyId, String permissionCode) {
        Optional<PermissionEntity> permission = permissionRepository.findByCode(permissionCode);
        if (permission.isEmpty()) {
            return List.of();
        }
        List<RolePermissionEntity> rolePerms =
                rolePermissionRepository.findByPermissionId(permission.get().getId());
        if (rolePerms.isEmpty()) {
            return List.of();
        }
        Set<UUID> roleIds = rolePerms.stream()
                .map(RolePermissionEntity::getRoleId)
                .collect(Collectors.toSet());
        Set<UUID> companyRoleIds = roleRepository.findAllById(roleIds).stream()
                .filter(r -> companyId.equals(r.getCompanyId()) && r.isActive())
                .map(RoleEntity::getId)
                .collect(Collectors.toSet());
        if (companyRoleIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> userIds = userRoleRepository.findByRoleIdIn(companyRoleIds).stream()
                .map(UserRoleEntity::getUserId)
                .collect(Collectors.toCollection(HashSet::new));
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(userIds).stream()
                .filter(u -> companyId.equals(u.getCompanyId()) && u.isActive())
                .toList();
    }

    private boolean userHasPermission(UUID userId, UUID companyId, String permissionCode) {
        Optional<PermissionEntity> permission = permissionRepository.findByCode(permissionCode);
        if (permission.isEmpty()) {
            return false;
        }
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            return false;
        }
        Set<UUID> roleIds = userRoles.stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toSet());
        Set<UUID> companyRoleIds = roleRepository.findAllById(roleIds).stream()
                .filter(r -> companyId.equals(r.getCompanyId()) && r.isActive())
                .map(RoleEntity::getId)
                .collect(Collectors.toSet());
        if (companyRoleIds.isEmpty()) {
            return false;
        }
        UUID permissionId = permission.get().getId();
        return rolePermissionRepository.findByRoleIdIn(companyRoleIds).stream()
                .anyMatch(rp -> permissionId.equals(rp.getPermissionId()));
    }

    private Optional<AppUserEntity> findUser(UUID companyId, String assigneeId) {
        try {
            UUID id = UUID.fromString(assigneeId);
            return userRepository.findById(id)
                    .filter(u -> companyId.equals(u.getCompanyId()));
        } catch (IllegalArgumentException ignored) {
            // not a UUID — treat as username
        }
        return userRepository.findByCompanyIdAndUsername(companyId, assigneeId);
    }

    private static ActivityAssigneeResponse toResponse(AppUserEntity u) {
        return new ActivityAssigneeResponse(
                assigneeKey(u),
                u.getUsername(),
                UserDisplayNameService.labelOf(u));
    }

    private static String sortLabel(AppUserEntity u) {
        return UserDisplayNameService.labelOf(u).toLowerCase(Locale.ROOT);
    }
}
