package com.jalaldeveloper.accountingsystem.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

    public static final String CLAIM_COMPANY_ID = "cid";

    private final PlatformSecurityProperties properties;
    private SecretKey key;

    public JwtService(PlatformSecurityProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initKey() {
        byte[] bytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (properties.isEnabled() && bytes.length < 32) {
            throw new IllegalStateException(
                    "app.security.jwt.secret must be at least 32 bytes when app.security.enabled=true");
        }
        this.key = Keys.hmacShaKeyFor(bytes.length >= 32 ? bytes : pad(bytes, 32));
    }

    private static byte[] pad(byte[] in, int len) {
        byte[] out = new byte[len];
        System.arraycopy(in, 0, out, 0, Math.min(in.length, len));
        return out;
    }

    public String createAccessToken(UUID userId, UUID companyId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getJwt().getExpirationMinutes() * 60);
        return Jwts.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(userId.toString())
                .claim(CLAIM_COMPANY_ID, companyId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
