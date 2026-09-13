package com.jalaldeveloper.accountingsystem.platform.security;

import com.jalaldeveloper.accountingsystem.application.exception.LocalizedResponseStatusException;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AppUserEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.AppUserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PlatformAuthenticationService {

    private final AppUserJpaRepository appUserJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PlatformSecurityProperties securityProperties;

    public PlatformAuthenticationService(
            AppUserJpaRepository appUserJpaRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PlatformSecurityProperties securityProperties) {
        this.appUserJpaRepository = appUserJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    public LoginResult login(UUID companyId, String username, String password) {
        if (!securityProperties.isEnabled()) {
            throw new LocalizedResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "error.auth.jwtDisabled",
                    "JWT login is disabled (app.security.enabled=false)");
        }
        AppUserEntity user = resolveUser(companyId, username)
                .orElseThrow(() -> new LocalizedResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "error.auth.invalidCredentials", "Invalid credentials"));
        if (!user.isActive()) {
            throw new LocalizedResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "error.auth.userInactive", "User inactive");
        }
        String hash = user.getPasswordHash();
        if (hash == null || hash.isBlank() || !passwordEncoder.matches(password, hash)) {
            throw new LocalizedResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "error.auth.invalidCredentials", "Invalid credentials");
        }
        String token = jwtService.createAccessToken(user.getId(), user.getCompanyId());
        long expSec = securityProperties.getJwt().getExpirationMinutes() * 60L;
        return new LoginResult(token, expSec, user.getId(), user.getCompanyId());
    }

    private java.util.Optional<AppUserEntity> resolveUser(UUID companyId, String username) {
        if (companyId != null) {
            return appUserJpaRepository.findByCompanyIdAndUsername(companyId, username);
        }
        return appUserJpaRepository.findFirstByUsernameIgnoreCaseOrderByCompanyIdAsc(username.trim());
    }

    public record LoginResult(String accessToken, long expiresInSeconds, UUID userId, UUID companyId) {}
}
