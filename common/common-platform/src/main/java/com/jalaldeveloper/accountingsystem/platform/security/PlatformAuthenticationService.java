package com.jalaldeveloper.accountingsystem.platform.security;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AppUserEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.AppUserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "JWT login is disabled (app.security.enabled=false)");
        }
        AppUserEntity user = appUserJpaRepository
                .findByCompanyIdAndUsername(companyId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User inactive");
        }
        String hash = user.getPasswordHash();
        if (hash == null || hash.isBlank() || !passwordEncoder.matches(password, hash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String token = jwtService.createAccessToken(user.getId(), user.getCompanyId());
        long expSec = securityProperties.getJwt().getExpirationMinutes() * 60L;
        return new LoginResult(token, expSec, user.getId(), user.getCompanyId());
    }

    public record LoginResult(String accessToken, long expiresInSeconds, UUID userId, UUID companyId) {}
}
