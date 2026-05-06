package com.jalaldeveloper.accountingsystem.platform.web;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.UserId;
import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * Called from {@link CompanyContextFilter} using the raw {@link HttpServletRequest}; does not
     * rely on {@link RequestContextHolder}.
     */
    void applyFromIncomingRequest(HttpServletRequest request, Optional<CompanyId> company, Optional<UserId> user) {
        if (company.isPresent()) {
            request.setAttribute(ATTR_COMPANY_ID, company.get());
        } else {
            request.removeAttribute(ATTR_COMPANY_ID);
        }
        if (user.isPresent()) {
            request.setAttribute(ATTR_USER_ID, user.get());
        } else {
            request.removeAttribute(ATTR_USER_ID);
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

    public String currentUserDisplay() {
        return currentUser().map(u -> u.getId().toString()).orElse("system");
    }

    private static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return null;
        }
        return servletAttrs.getRequest();
    }
}
