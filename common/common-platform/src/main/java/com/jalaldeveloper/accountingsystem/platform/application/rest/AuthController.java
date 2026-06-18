package com.jalaldeveloper.accountingsystem.platform.application.rest;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AppUserEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.PermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RolePermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.UserRoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.AppUserJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.PermissionJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RoleJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RolePermissionJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.UserRoleJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.security.PlatformAuthenticationService;
import com.jalaldeveloper.accountingsystem.platform.security.PlatformSecurityProperties;
import com.jalaldeveloper.accountingsystem.platform.settings.UpdateProfileRequest;
import com.jalaldeveloper.accountingsystem.platform.settings.UserApplicationService;
import com.jalaldeveloper.accountingsystem.platform.settings.UserResponse;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = "application/json")
public class AuthController {

    private final PlatformAuthenticationService authenticationService;
    private final PlatformSecurityProperties securityProperties;
    private final UserApplicationService userApplicationService;
    private final CompanyContext companyContext;
    private final AppUserJpaRepository appUserRepository;
    private final UserRoleJpaRepository userRoleRepository;
    private final RoleJpaRepository roleRepository;
    private final RolePermissionJpaRepository rolePermissionRepository;
    private final PermissionJpaRepository permissionRepository;

    public AuthController(
            PlatformAuthenticationService authenticationService,
            PlatformSecurityProperties securityProperties,
            UserApplicationService userApplicationService,
            CompanyContext companyContext,
            AppUserJpaRepository appUserRepository,
            UserRoleJpaRepository userRoleRepository,
            RoleJpaRepository roleRepository,
            RolePermissionJpaRepository rolePermissionRepository,
            PermissionJpaRepository permissionRepository) {
        this.authenticationService = authenticationService;
        this.securityProperties = securityProperties;
        this.userApplicationService = userApplicationService;
        this.companyContext = companyContext;
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest body) {
        PlatformAuthenticationService.LoginResult r =
                authenticationService.login(body.companyId(), body.username(), body.password());
        return ResponseEntity.ok(new LoginResponse(
                r.accessToken(),
                "Bearer",
                r.expiresInSeconds(),
                r.userId(),
                r.companyId()));
    }

    /** Public summary of security configuration (no secrets). */
    @GetMapping("/security-config")
    public ResponseEntity<SecurityConfigResponse> securityConfig() {
        return ResponseEntity.ok(new SecurityConfigResponse(
                securityProperties.isEnabled(),
                securityProperties.getAuthorization().isStrict(),
                securityProperties.getJwt().getIssuer(),
                securityProperties.getJwt().getExpirationMinutes(),
                maskSecret(securityProperties.getJwt().getSecret())));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me() {
        UUID userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.ok(MeResponse.anonymous());
        }
        AppUserEntity user = appUserRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(MeResponse.anonymous());
        }
        List<UUID> roleIds = userRoleRepository.findByUserId(userId).stream()
                .map(UserRoleEntity::getRoleId).toList();
        List<RoleSummary> roles = roleRepository.findAllById(roleIds).stream()
                .sorted(Comparator.comparing(RoleEntity::getName))
                .map(r -> new RoleSummary(r.getId(), r.getCode(), r.getName()))
                .toList();
        List<UUID> permissionIds = roleIds.isEmpty()
                ? List.of()
                : rolePermissionRepository.findByRoleIdIn(roleIds).stream()
                        .map(RolePermissionEntity::getPermissionId).distinct().toList();
        List<String> permissionCodes = permissionIds.isEmpty()
                ? List.of()
                : permissionRepository.findAllById(permissionIds).stream()
                        .map(PermissionEntity::getCode)
                        .sorted()
                        .collect(Collectors.toList());

        UUID companyId = companyContext.currentCompany().map(c -> c.getId()).orElse(user.getCompanyId());
        return ResponseEntity.ok(new MeResponse(
                user.getId(),
                companyId,
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.isActive(),
                roles,
                Set.copyOf(permissionCodes)));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest body) {
        UUID userId = requireUserId();
        return ResponseEntity.ok(userApplicationService.updateOwnProfile(userId, body));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest body) {
        UUID userId = requireUserId();
        boolean ok = userApplicationService.changeOwnPassword(userId, body.currentPassword(), body.newPassword());
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId() {
        return companyContext.currentUser().map(u -> u.getId()).orElse(null);
    }

    private UUID requireUserId() {
        UUID id = currentUserId();
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return id;
    }

    private static String maskSecret(String secret) {
        if (secret == null || secret.length() < 8) {
            return "(not set)";
        }
        return secret.substring(0, 4) + "…" + secret.substring(secret.length() - 2);
    }

    public record LoginRequest(
            UUID companyId,
            @NotBlank String username,
            @NotBlank String password) {}

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresInSeconds,
            UUID userId,
            UUID companyId) {}

    public record SecurityConfigResponse(
            boolean securityEnabled,
            boolean authorizationStrict,
            String jwtIssuer,
            long jwtExpirationMinutes,
            String jwtSecretPreview) {}

    public record RoleSummary(UUID id, String code, String name) {}

    public record MeResponse(
            UUID userId,
            UUID companyId,
            String username,
            String email,
            String displayName,
            boolean active,
            List<RoleSummary> roles,
            Set<String> permissions) {

        static MeResponse anonymous() {
            return new MeResponse(null, null, null, null, null, false, List.of(), Set.of());
        }
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 200) String newPassword) {}
}
