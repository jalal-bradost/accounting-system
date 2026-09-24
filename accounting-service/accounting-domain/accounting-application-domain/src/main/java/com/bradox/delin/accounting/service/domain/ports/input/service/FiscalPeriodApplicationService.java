package com.bradox.delin.accounting.service.domain.ports.input.service;

import com.bradox.delin.accounting.service.domain.ports.output.repository.FiscalPeriodRepository.FiscalPeriodInfo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FiscalPeriodApplicationService {

    FiscalPeriodInfo createPeriod(UUID companyId, LocalDate startDate, LocalDate endDate);

    List<FiscalPeriodInfo> listPeriods(UUID companyId);

    List<FiscalPeriodInfo> ensureMonthlyPeriods(UUID companyId, int year);

    CloseChecklist getCloseChecklist(UUID companyId, UUID periodId);

    FiscalPeriodInfo closeMonth(UUID companyId, UUID periodId, UUID closedBy);

    List<FiscalPeriodInfo> closeYear(UUID companyId, int year, UUID closedBy);

    record CloseChecklist(
            UUID periodId,
            LocalDate startDate,
            LocalDate endDate,
            boolean open,
            int draftJournalEntries,
            int draftCustomerInvoices,
            int draftVendorBills,
            boolean canClose
    ) {}
}
