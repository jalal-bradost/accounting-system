package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Output port for fiscal period lookup. Used to ensure entries are posted only in open periods.
 */
public interface FiscalPeriodRepository {

    /**
     * Returns the fiscal period that contains the given date for the company, if any.
     * Used to check that the period exists and is open before posting.
     */
    Optional<FiscalPeriodInfo> findPeriodContaining(CompanyId companyId, LocalDate date);

    /**
     * Lists all fiscal periods for the company, newest first.
     */
    List<FiscalPeriodInfo> findByCompanyIdOrderByStartDateDesc(CompanyId companyId);

    /**
     * Minimal read-only info about a fiscal period for validation.
     */
    record FiscalPeriodInfo(java.util.UUID id, LocalDate startDate, LocalDate endDate, boolean open) {}

    /**
     * Creates a fiscal period. Returns the created period info.
     */
    FiscalPeriodInfo create(CompanyId companyId, LocalDate startDate, LocalDate endDate, boolean open);
}
