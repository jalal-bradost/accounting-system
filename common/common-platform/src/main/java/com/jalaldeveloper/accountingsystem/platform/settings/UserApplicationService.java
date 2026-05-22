package com.jalaldeveloper.accountingsystem.platform.settings;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AppUserEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.UserRoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.AppUserJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RoleJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.UserRoleJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserApplicationService {

    private final AppUserJpaRepository userRepository;
    private final RoleJpaRepository roleRepository;
    private final UserRoleJpaRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserApplicationService(AppUserJpaRepository userRepository,
                                  RoleJpaRepository roleRepository,
                                  UserRoleJpaRepository userRoleRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(UUID companyId, String q, Boolean active, Pageable pageable) {
        Page<AppUserEntity> page = userRepository.search(companyId, q, active, pageable);
        Map<UUID, List<UserResponse.RoleSummary>> rolesByUser = loadRoles(page.getContent().stream().map(AppUserEntity::getId).toList());
        return page.map(u -> UserResponse.of(u, rolesByUser.getOrDefault(u.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        AppUserEntity u = load(id);
        return UserResponse.of(u, loadRolesForUser(u.getId()));
    }

    @Transactional
    public UserResponse create(UUID companyId, CreateUserRequest req) {
        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company context required");
        }
        userRepository.findByCompanyIdAndUsername(companyId, req.username()).ifPresent(u -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        });
        userRepository.findByCompanyIdAndEmail(companyId, req.email()).ifPresent(u -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        });

        AppUserEntity u = new AppUserEntity();
        u.setId(UUID.randomUUID());
        u.setCompanyId(companyId);
        u.setUsername(req.username().trim());
        u.setEmail(req.email().trim());
        u.setDisplayName(blankToNull(req.displayName()));
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setActive(true);
        userRepository.save(u);

        if (req.roleIds() != null && !req.roleIds().isEmpty()) {
            replaceRoles(u, req.roleIds());
        }
        return UserResponse.of(u, loadRolesForUser(u.getId()));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest req) {
        AppUserEntity u = load(id);
        if (req.email() != null && !req.email().equalsIgnoreCase(u.getEmail())) {
            userRepository.findByCompanyIdAndEmail(u.getCompanyId(), req.email()).ifPresent(other -> {
                if (!other.getId().equals(u.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                }
            });
            u.setEmail(req.email().trim());
        }
        if (req.displayName() != null) {
            u.setDisplayName(blankToNull(req.displayName()));
        }
        userRepository.save(u);
        return UserResponse.of(u, loadRolesForUser(u.getId()));
    }

    @Transactional
    public UserResponse setRoles(UUID id, Set<UUID> roleIds) {
        AppUserEntity u = load(id);
        replaceRoles(u, roleIds);
        return UserResponse.of(u, loadRolesForUser(u.getId()));
    }

    @Transactional
    public UserResponse setActive(UUID id, boolean active) {
        AppUserEntity u = load(id);
        u.setActive(active);
        if (!active) {
            u.setArchivedAt(Instant.now());
        } else {
            u.setArchivedAt(null);
            u.setArchivedBy(null);
        }
        userRepository.save(u);
        return UserResponse.of(u, loadRolesForUser(u.getId()));
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        AppUserEntity u = load(id);
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(u);
    }

    @Transactional
    public void delete(UUID id) {
        AppUserEntity u = load(id);
        u.setActive(false);
        u.setArchivedAt(Instant.now());
        userRepository.save(u);
    }

    @Transactional
    public boolean changeOwnPassword(UUID userId, String currentPassword, String newPassword) {
        AppUserEntity u = load(userId);
        if (u.getPasswordHash() == null || !passwordEncoder.matches(currentPassword, u.getPasswordHash())) {
            return false;
        }
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        return true;
    }

    @Transactional
    public UserResponse updateOwnProfile(UUID userId, UpdateProfileRequest req) {
        AppUserEntity u = load(userId);
        if (req.displayName() != null) {
            u.setDisplayName(blankToNull(req.displayName()));
        }
        if (req.email() != null && !req.email().equalsIgnoreCase(u.getEmail())) {
            userRepository.findByCompanyIdAndEmail(u.getCompanyId(), req.email()).ifPresent(other -> {
                if (!other.getId().equals(u.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                }
            });
            u.setEmail(req.email().trim());
        }
        userRepository.save(u);
        return UserResponse.of(u, loadRolesForUser(u.getId()));
    }

    private AppUserEntity load(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Map<UUID, List<UserResponse.RoleSummary>> loadRoles(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<UserRoleEntity> links = userRoleRepository.findByUserIdIn(userIds);
        if (links.isEmpty()) {
            return Map.of();
        }
        Set<UUID> roleIds = links.stream().map(UserRoleEntity::getRoleId).collect(Collectors.toSet());
        Map<UUID, RoleEntity> rolesById = roleRepository.findAllById(roleIds).stream()
                .collect(Collectors.toMap(RoleEntity::getId, r -> r));
        Map<UUID, List<UserResponse.RoleSummary>> grouped = new HashMap<>();
        for (UserRoleEntity link : links) {
            RoleEntity r = rolesById.get(link.getRoleId());
            if (r == null) continue;
            grouped.computeIfAbsent(link.getUserId(), k -> new java.util.ArrayList<>())
                    .add(new UserResponse.RoleSummary(r.getId(), r.getCode(), r.getName()));
        }
        return grouped;
    }

    private List<UserResponse.RoleSummary> loadRolesForUser(UUID userId) {
        return loadRoles(List.of(userId)).getOrDefault(userId, List.of());
    }

    private void replaceRoles(AppUserEntity user, Set<UUID> roleIds) {
        userRoleRepository.deleteByUserId(user.getId());
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        // Sanity: make sure each role is in the same company.
        Map<UUID, RoleEntity> roles = roleRepository.findAllById(roleIds).stream()
                .collect(Collectors.toMap(RoleEntity::getId, r -> r, (a, b) -> a, LinkedHashMap::new));
        for (UUID roleId : roleIds) {
            RoleEntity role = roles.get(roleId);
            if (role == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + roleId);
            }
            if (!role.getCompanyId().equals(user.getCompanyId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Role " + role.getCode() + " does not belong to user's company");
            }
            userRoleRepository.save(new UserRoleEntity(user.getId(), role.getId()));
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
