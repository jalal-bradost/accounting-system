package com.bradox.erp.accounting.service.domain.ports.output.settings;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Output port for company-level accounting calendar settings.
 * Posting is not allowed for dates on or before the period lock date.
 */
public interface CompanyLockDatePort {

    Optional<LocalDate> getPeriodLockDate(CompanyId companyId);

    void setPeriodLockDate(CompanyId companyId, LocalDate periodLockDate);

    /**
     * Fiscal year start month (1–12). Defaults to January when unset.
     */
    int getFiscalYearStartMonth(CompanyId companyId);
}
