package com.bradox.erp.platform.web;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Holds the current company and user for the active HTTP request. Values are stored as
 * {@linkplain HttpServletRequest#setAttribute servlet request attributes} (see attribute keys below)
 * so {@link CompanyContextFilter} can populate them before {@code RequestContextFilter} runs;
 * controller and service code then reads them via {@link RequestContextHolder}.
 *
 * <p>Outside of a web request (background jobs, application startup), {@link #currentCompany()}
 * is empty and callers should fall back to explicit company arguments.
 */
@Component
public class CompanyContext {

    static final String ATTR_COMPANY_ID = CompanyContext.class.getName() + ".companyId";
    static final String ATTR_USER_ID = CompanyContext.class.getName() + ".userId";
    static final String ATTR_USER_DISPLAY = CompanyContext.class.getName() + ".userDisplay";

    private final ObjectProvider<UserDisplayNameService> displayNameServiceProvider;

    public CompanyContext(ObjectProvider<UserDisplayNameService> displayNameServiceProvider) {
        this.displayNameServiceProvider = displayNameServiceProvider;
    }

    /**
     * Called from {@link CompanyContextFilter} and JWT authentication using the raw {@link HttpServletRequest};
     * does not rely on {@link RequestContextHolder}.
     */
    public void applyFromIncomingRequest(HttpServletRequest request, Optional<CompanyId> company, Optional<UserId> user) {
        if (company.isPresent()) {
            request.setAttribute(ATTR_COMPANY_ID, company.get());
        } else {
            request.removeAttribute(ATTR_COMPANY_ID);
        }
        if (user.isPresent()) {
            request.setAttribute(ATTR_USER_ID, user.get());
            request.removeAttribute(ATTR_USER_DISPLAY);
        } else {
            request.removeAttribute(ATTR_USER_ID);
            request.removeAttribute(ATTR_USER_DISPLAY);
        }
    }

    /** Optional pre-resolved display label (e.g. from JWT auth after loading the user). */
    public void applyUserDisplay(HttpServletRequest request, String display) {
        if (display != null && !display.isBlank()) {
            request.setAttribute(ATTR_USER_DISPLAY, display.trim());
        } else {
            request.removeAttribute(ATTR_USER_DISPLAY);
        }
    }

    public Optional<CompanyId> currentCompany() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return Optional.empty();
        }
        Object v = req.getAttribute(ATTR_COMPANY_ID);
        if (v instanceof CompanyId id) {
            return Optional.of(id);
        }
        return Optional.empty();
    }

    public Optional<UserId> currentUser() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return Optional.empty();
        }
        Object v = req.getAttribute(ATTR_USER_ID);
        if (v instanceof UserId id) {
            return Optional.of(id);
        }
        return Optional.empty();
    }

    public CompanyId requireCompany() {
        return currentCompany()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No current company on the request. Provide X-Company-Id header or ?companyId= parameter."));
    }

    /**
     * Human-readable label for the current user (display name, else username). Used for
     * validated-by, chatter authors, audit, etc.
     */
    public String currentUserDisplay() {
        HttpServletRequest req = currentRequest();
        if (req != null) {
            Object cached = req.getAttribute(ATTR_USER_DISPLAY);
            if (cached instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        Optional<UserId> user = currentUser();
        if (user.isEmpty()) {
            return "system";
        }
        UserDisplayNameService names = displayNameServiceProvider.getIfAvailable();
        String label = names != null
                ? names.forUserId(user.get().getId())
                : user.get().getId().toString();
        if (req != null) {
            req.setAttribute(ATTR_USER_DISPLAY, label);
        }
        return label;
    }

    /** Resolve a stored user ref (UUID or already a name) to a display label. */
    public String resolveUserDisplay(String stored) {
        if (stored == null || stored.isBlank() || "system".equals(stored)) {
            return stored;
        }
        UserDisplayNameService names = displayNameServiceProvider.getIfAvailable();
        return names != null ? names.resolve(stored) : stored;
    }

    private static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return null;
        }
        return servletAttrs.getRequest();
    }
}
