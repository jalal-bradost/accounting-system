package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.settings;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Output port for company-level lock date. Posting is not allowed for entries with date before this.
 */
public interface CompanyLockDatePort {

    Optional<LocalDate> getPeriodLockDate(CompanyId companyId);

    void setPeriodLockDate(CompanyId companyId, LocalDate periodLockDate);
}
