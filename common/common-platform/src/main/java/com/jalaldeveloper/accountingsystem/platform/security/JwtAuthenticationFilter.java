package com.jalaldeveloper.accountingsystem.platform.security;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.UserId;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
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
 */
@Component
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CompanyContext companyContext;

    public JwtAuthenticationFilter(JwtService jwtService, CompanyContext companyContext) {
        this.jwtService = jwtService;
        this.companyContext = companyContext;
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
            if (cid == null || cid.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }
            UUID companyUuid = UUID.fromString(cid);
            UserId uid = new UserId(userId);
            CompanyId companyId = new CompanyId(companyUuid);
            companyContext.applyFromIncomingRequest(request, Optional.of(companyId), Optional.of(uid));
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
}
