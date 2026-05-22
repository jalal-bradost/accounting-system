package com.jalaldeveloper.accountingsystem.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Central security and authorization rule configuration ({@code app.security.*}).
 * When {@link #enabled} is {@code false} (default), API requests behave as today: no JWT required and
 * missing {@code X-User-Id} is treated as permissive for {@link AuthorizationPort} unless strict mode is on.
 */
@ConfigurationProperties(prefix = "app.security")
public class PlatformSecurityProperties {

    /**
     * When true, {@code /api/**} requires a valid JWT (except {@code /api/v1/auth/login} and public config).
     */
    private boolean enabled = false;

    private final Jwt jwt = new Jwt();

    private final Authorization authorization = new Authorization();

    /** Plaintext password for the seeded demo admin (only used when seeding runs). */
    private String seedDefaultAdminPassword = "admin";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Authorization getAuthorization() {
        return authorization;
    }

    public String getSeedDefaultAdminPassword() {
        return seedDefaultAdminPassword;
    }

    public void setSeedDefaultAdminPassword(String seedDefaultAdminPassword) {
        this.seedDefaultAdminPassword = seedDefaultAdminPassword;
    }

    public static class Jwt {
        /** HS256 secret — must be at least 256 bits (32 ASCII chars) when security is enabled. */
        private String secret = "dev-only-change-me-please-use-32b-minimum-secret!!";

        private String issuer = "accounting-system";

        private long expirationMinutes = 720;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }

    /**
     * Rule-based authorization layered on top of RBAC: explicit denies and superuser bypass.
     */
    public static class Authorization {
        /**
         * When security is enabled, anonymous requests fail authentication first. For permission checks,
         * if true, a missing user id results in deny instead of allow.
         */
        private boolean strict = true;

        /** Permission codes removed from the effective set after RBAC resolution (exact match). */
        private List<String> denyPermissionCodes = new ArrayList<>();

        /** User ids that bypass permission checks (use sparingly; audit-sensitive). */
        private List<UUID> grantAllUserIds = new ArrayList<>();

        public boolean isStrict() {
            return strict;
        }

        public void setStrict(boolean strict) {
            this.strict = strict;
        }

        public List<String> getDenyPermissionCodes() {
            return denyPermissionCodes;
        }

        public void setDenyPermissionCodes(List<String> denyPermissionCodes) {
            this.denyPermissionCodes = denyPermissionCodes != null ? denyPermissionCodes : new ArrayList<>();
        }

        public List<UUID> getGrantAllUserIds() {
            return grantAllUserIds;
        }

        public void setGrantAllUserIds(List<UUID> grantAllUserIds) {
            this.grantAllUserIds = grantAllUserIds != null ? grantAllUserIds : new ArrayList<>();
        }
    }
}
