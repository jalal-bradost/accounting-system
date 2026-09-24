package com.bradox.delin.platform.security;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.domain.valueobject.UserId;
import com.bradox.delin.platform.dataaccess.repository.AppUserJpaRepository;
import com.bradox.delin.platform.web.CompanyContext;
import com.bradox.delin.platform.web.CompanyContextFilter;
import com.bradox.delin.platform.web.UserDisplayNameService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates Bearer JWTs and binds {@link CompanyContext} + Spring {@link SecurityContextHolder}.
 * The token always identifies the user. {@code X-Company-Id} selects the active tenant when
 * present; otherwise the company claim on the token is used.
 */
@Component
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CompanyContext companyContext;
    private final AppUserJpaRepository appUserRepository;
    private final UserDisplayNameService userDisplayNameService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CompanyContext companyContext,
            AppUserJpaRepository appUserRepository,
            UserDisplayNameService userDisplayNameService) {
        this.jwtService = jwtService;
        this.companyContext = companyContext;
        this.appUserRepository = appUserRepository;
        this.userDisplayNameService = userDisplayNameService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            filterChain.doFilter(request, response);
            return;
        }
        String raw = auth.substring(7).trim();
        if (raw.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtService.parseAndValidate(raw);
            UUID userId = UUID.fromString(claims.getSubject());
            String cid = claims.get(JwtService.CLAIM_COMPANY_ID, String.class);
            String headerCid = request.getHeader(CompanyContextFilter.HEADER_COMPANY_ID);
            UUID companyUuid = parseUuid(headerCid).orElseGet(() -> parseUuid(cid).orElse(null));
            if (companyUuid == null) {
                filterChain.doFilter(request, response);
                return;
            }
            UserId uid = new UserId(userId);
            CompanyId companyId = new CompanyId(companyUuid);
            companyContext.applyFromIncomingRequest(request, Optional.of(companyId), Optional.of(uid));
            appUserRepository.findById(userId).ifPresent(user -> {
                String label = UserDisplayNameService.labelOf(user);
                userDisplayNameService.putCached(userId, label);
                companyContext.applyUserDisplay(request, label);
            });
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    uid.getId().toString(),
                    null,
                    AuthorityUtils.NO_AUTHORITIES);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"INVALID_TOKEN\",\"message\":\"Invalid or expired access token\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
