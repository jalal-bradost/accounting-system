package com.jalaldeveloper.accountingsystem.platform.settings;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.PermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RolePermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.PermissionJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RoleJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RolePermissionJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.UserRoleJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Custom roles per company with full permission management. The seeder-created
 * roles ({@code ADMIN}/{@code ACCOUNTANT}/...) are reported as {@code systemRole=true}
 * but remain editable by callers that hold {@code platform.role.write}.
 */
@Service
public class RoleApplicationService {

    /** Codes seeded in {@code PlatformRbacSeeder}; flagged in responses so the UI can warn. */
    public static final Set<String> SYSTEM_ROLE_CODES = Set.of(
            "ADMIN", "ACCOUNTANT", "SALES", "PURCHASING", "WAREHOUSE", "READONLY");

    private final RoleJpaRepository roleRepository;
    private final PermissionJpaRepository permissionRepository;
    private final RolePermissionJpaRepository rolePermissionRepository;
    private final UserRoleJpaRepository userRoleRepository;

    public RoleApplicationService(RoleJpaRepository roleRepository,
                                  PermissionJpaRepository permissionRepository,
                                  RolePermissionJpaRepository rolePermissionRepository,
                                  UserRoleJpaRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list(UUID companyId) {
        List<RoleEntity> roles = roleRepository.findByCompanyId(companyId);
        if (roles.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<RolePermissionEntity>> linksByRole = rolePermissionRepository
                .findByRoleIdIn(roles.stream().map(RoleEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(RolePermissionEntity::getRoleId));
        Set<UUID> permissionIds = linksByRole.values().stream().flatMap(List::stream)
                .map(RolePermissionEntity::getPermissionId).collect(Collectors.toSet());
        Map<UUID, PermissionEntity> permsById = permissionRepository.findAllById(permissionIds).stream()
                .collect(Collectors.toMap(PermissionEntity::getId, p -> p));

        return roles.stream().map(r -> {
            List<RolePermissionEntity> links = linksByRole.getOrDefault(r.getId(), List.of());
            List<UUID> ids = links.stream().map(RolePermissionEntity::getPermissionId).toList();
            List<String> codes = ids.stream()
                    .map(permsById::get).filter(p -> p != null)
                    .map(PermissionEntity::getCode).sorted().toList();
            int userCount = (int) userRoleRepository.countByRoleId(r.getId());
            return RoleResponse.of(r, SYSTEM_ROLE_CODES.contains(r.getCode()), userCount, ids, codes);
        }).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse get(UUID id) {
        RoleEntity r = load(id);
        return buildResponse(r);
    }

    @Transactional
    public RoleResponse create(UUID companyId, RoleWriteRequest req) {
        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company context required");
        }
        String code = req.code().trim().toUpperCase();
        if (roleRepository.existsByCompanyIdAndCode(companyId, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role code already exists");
        }
        RoleEntity r = new RoleEntity();
        r.setId(UUID.randomUUID());
        r.setCompanyId(companyId);
        r.setCode(code);
        r.setName(req.name().trim());
        r.setDescription(blankToNull(req.description()));
        r.setActive(true);
        roleRepository.save(r);
        if (req.permissionIds() != null && !req.permissionIds().isEmpty()) {
            replacePermissions(r.getId(), req.permissionIds());
        }
        return buildResponse(r);
    }

    @Transactional
    public RoleResponse update(UUID id, RoleWriteRequest req) {
        RoleEntity r = load(id);
        // Code is immutable for system roles to keep seeded permissions stable.
        if (!SYSTEM_ROLE_CODES.contains(r.getCode())) {
            String code = req.code().trim().toUpperCase();
            if (!code.equals(r.getCode()) && roleRepository.existsByCompanyIdAndCode(r.getCompanyId(), code)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Role code already exists");
            }
            r.setCode(code);
        }
        r.setName(req.name().trim());
        r.setDescription(blankToNull(req.description()));
        roleRepository.save(r);
        if (req.permissionIds() != null) {
            replacePermissions(r.getId(), req.permissionIds());
        }
        return buildResponse(r);
    }

    @Transactional
    public RoleResponse setPermissions(UUID id, Set<UUID> permissionIds) {
        RoleEntity r = load(id);
        replacePermissions(r.getId(), permissionIds);
        return buildResponse(r);
    }

    @Transactional
    public void delete(UUID id) {
        RoleEntity r = load(id);
        if (SYSTEM_ROLE_CODES.contains(r.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "System roles cannot be deleted");
        }
        long users = userRoleRepository.countByRoleId(id);
        if (users > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Role is assigned to " + users + " user(s); remove assignments first");
        }
        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.delete(r);
    }

    private RoleEntity load(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    private RoleResponse buildResponse(RoleEntity r) {
        List<RolePermissionEntity> links = rolePermissionRepository.findByRoleId(r.getId());
        List<UUID> ids = links.stream().map(RolePermissionEntity::getPermissionId).toList();
        Map<UUID, PermissionEntity> permsById = permissionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(PermissionEntity::getId, p -> p));
        List<String> codes = ids.stream()
                .map(permsById::get).filter(p -> p != null)
                .map(PermissionEntity::getCode).sorted().toList();
        int userCount = (int) userRoleRepository.countByRoleId(r.getId());
        return RoleResponse.of(r, SYSTEM_ROLE_CODES.contains(r.getCode()), userCount, ids, codes);
    }

    private void replacePermissions(UUID roleId, Set<UUID> permissionIds) {
        rolePermissionRepository.deleteByRoleId(roleId);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        Set<UUID> known = permissionRepository.findAllById(permissionIds).stream()
                .map(PermissionEntity::getId).collect(Collectors.toSet());
        Set<UUID> unknown = new HashSet<>(permissionIds);
        unknown.removeAll(known);
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown permission ids: " + unknown);
        }
        Map<UUID, RolePermissionEntity> seen = new HashMap<>();
        for (UUID pid : permissionIds) {
            seen.computeIfAbsent(pid, p -> rolePermissionRepository.save(new RolePermissionEntity(roleId, p)));
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
