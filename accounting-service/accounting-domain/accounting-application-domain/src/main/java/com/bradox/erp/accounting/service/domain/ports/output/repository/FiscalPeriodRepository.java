package com.bradox.erp.accounting.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for fiscal period lookup. Used to ensure entries are posted only in open periods.
 */
public interface FiscalPeriodRepository {

    /**
     * Returns the fiscal period that contains the given date for the company, if any.
     * Used to check that the period exists and is open before posting.
     */
    Optional<FiscalPeriodInfo> findPeriodContaining(CompanyId companyId, LocalDate date);

    Optional<FiscalPeriodInfo> findById(CompanyId companyId, UUID periodId);

    /**
     * Lists all fiscal periods for the company, newest first.
     */
    List<FiscalPeriodInfo> findByCompanyIdOrderByStartDateDesc(CompanyId companyId);

    /**
     * Minimal read-only info about a fiscal period for validation and close workflow.
     */
    record FiscalPeriodInfo(
            UUID id,
            LocalDate startDate,
            LocalDate endDate,
            boolean open,
            Instant closedAt,
            UUID closedBy
    ) {
        public FiscalPeriodInfo(UUID id, LocalDate startDate, LocalDate endDate, boolean open) {
            this(id, startDate, endDate, open, null, null);
        }
    }

    /**
     * Creates an open fiscal period. Returns the created period info.
     */
    FiscalPeriodInfo create(CompanyId companyId, LocalDate startDate, LocalDate endDate);

    /**
     * Irreversibly closes a period. No-op if already closed. Never reopens.
     */
    FiscalPeriodInfo close(CompanyId companyId, UUID periodId, UUID closedBy, Instant closedAt);

    /**
     * Latest closed period end date for the company, if any.
     */
    Optional<LocalDate> findLatestClosedPeriodEndDate(CompanyId companyId);
}
