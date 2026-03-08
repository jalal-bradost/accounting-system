package com.jalaldeveloper.accountingsystem.web;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.UUID;

/**
 * Session-scoped company context for the dashboard.
 * Default company ID used when none is set (demo).
 */
@Component
@SessionScope
public class WebCompanyContext {

    private UUID companyId = DashboardController.DEFAULT_COMPANY_ID;

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }
}
