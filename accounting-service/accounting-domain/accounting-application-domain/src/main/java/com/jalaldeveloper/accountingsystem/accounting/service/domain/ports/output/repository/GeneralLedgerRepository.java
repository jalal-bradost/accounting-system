package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GeneralLedgerRepository {

    /**
     * Posted journal lines in date order: account, then entry date, then entry id.
     * When {@code accountId} is null, all accounts for the company are included.
     */
    List<GeneralLedgerRawLine> listPostedLines(CompanyId companyId, LocalDate from, LocalDate to, UUID accountId);

    record GeneralLedgerRawLine(
            UUID accountId,
            UUID journalEntryId,
            LocalDate entryDate,
            String journalCode,
            String sequenceNumber,
            String label,
            BigDecimal debit,
            BigDecimal credit
    ) {}
}
