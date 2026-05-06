package com.jalaldeveloper.accountingsystem.platform.audit;

import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Static accessor that lets the JPA {@link AuditingEntityListener} reach the audit
 * service and the per-request {@link CompanyContext}. JPA listeners cannot be
 * Spring beans themselves; this holder is wired once at startup.
 */
@Component
public class AuditContextHolder {

    private static AuditLogPort auditLogPort;
    private static ObjectProvider<CompanyContext> companyContextProvider;

    public AuditContextHolder(AuditLogPort port,
                              ObjectProvider<CompanyContext> companyContextProvider) {
        AuditContextHolder.auditLogPort = port;
        AuditContextHolder.companyContextProvider = companyContextProvider;
    }

    public static AuditLogPort auditLogPort() {
        return auditLogPort;
    }

    public static CompanyContext currentContext() {
        if (companyContextProvider == null) return null;
        try {
            return companyContextProvider.getIfAvailable();
        } catch (RuntimeException ex) {
            // No active request scope (e.g. seeders, async background tasks).
            return null;
        }
    }
}
