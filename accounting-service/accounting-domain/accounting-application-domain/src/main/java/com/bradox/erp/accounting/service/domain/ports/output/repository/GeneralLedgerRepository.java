package com.bradox.erp.accounting.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            LocalDateTime entryDate,
            String journalCode,
            String sequenceNumber,
            String label,
            BigDecimal debit,
            BigDecimal credit
    ) {}
}
