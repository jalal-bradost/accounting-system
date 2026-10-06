package com.bradox.erp.platform.settings;

import java.util.UUID;

/**
 * Hook implemented by the accounting/inventory container so every new company gets
 * the standard chart of accounts, journals, warehouse, UoMs, and default category.
 */
public interface CompanyErpBootstrapHandler {

    /** Idempotent seed of ERP defaults for {@code companyId}. */
    void bootstrap(UUID companyId);
}
