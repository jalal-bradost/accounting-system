package com.jalaldeveloper.accountingsystem.platform.web;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.UserId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Resolves the current company and user for each HTTP request and stores them on the
 * {@link HttpServletRequest} (see {@link CompanyContext}). This runs early in the chain and does
 * not depend on Spring request scope or {@code RequestContextFilter}.
 *
 * <p>Resolution precedence:
 * <ol>
 *   <li>{@code X-Company-Id} / {@code X-User-Id} headers (preferred path; aligns with
 *       the future Spring Security stack which can write these from the JWT principal).</li>
 *   <li>{@code companyId} / {@code userId} query parameters (back-compat with existing
 *       controllers that already accept companyId as a parameter).</li>
 * </ol>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CompanyContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CompanyContextFilter.class);

    public static final String HEADER_COMPANY_ID = "X-Company-Id";
    public static final String HEADER_USER_ID = "X-User-Id";

    private final CompanyContext companyContext;

    public CompanyContextFilter(CompanyContext companyContext) {
        this.companyContext = companyContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            companyContext.applyFromIncomingRequest(request, resolveCompany(request), resolveUser(request));
        } catch (RuntimeException ex) {
            log.warn("Failed to apply company context for {}: {}", request.getRequestURI(), ex.getMessage());
        }
        chain.doFilter(request, response);
    }

    private static java.util.Optional<CompanyId> resolveCompany(HttpServletRequest request) {
        String value = firstNonBlank(request.getHeader(HEADER_COMPANY_ID), request.getParameter("companyId"));
        return parseUuid(value).map(CompanyId::new);
    }

    private static java.util.Optional<UserId> resolveUser(HttpServletRequest request) {
        String value = firstNonBlank(request.getHeader(HEADER_USER_ID), request.getParameter("userId"));
        return parseUuid(value).map(UserId::new);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static java.util.Optional<UUID> parseUuid(String value) {
        if (value == null) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return java.util.Optional.empty();
        }
    }
}
