package com.bradox.erp.accounting.service.domain.ports.output.sequence;

import com.bradox.erp.domain.core.ValueObject.JournalId;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.LocalDate;

/**
 * Output port for generating unique journal entry sequence numbers per company/journal (and optionally period).
 */
public interface SequenceGeneratorPort {

    /**
     * Returns the next sequence number for a journal entry in the given company and journal for the given date.
     * Format is implementation-defined (e.g. "Bank/2026/00001").
     */
    String getNextSequenceNumber(CompanyId companyId, JournalId journalId, LocalDate forDate);
}
