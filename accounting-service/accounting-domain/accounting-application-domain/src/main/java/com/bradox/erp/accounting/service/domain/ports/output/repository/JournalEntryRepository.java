package com.bradox.erp.accounting.service.domain.ports.output.repository;

import com.bradox.erp.domain.core.entity.JournalEntry;
import com.bradox.erp.domain.core.ValueObject.JournalEntryId;
import com.bradox.erp.domain.core.ValueObject.JournalId;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Output port for JournalEntry persistence (hexagonal architecture).
 */
public interface JournalEntryRepository {

    JournalEntry save(JournalEntry journalEntry);

    Optional<JournalEntry> findById(JournalEntryId id);

    List<JournalEntry> findByCompanyId(CompanyId companyId);

    List<JournalEntry> findByCompanyIdAndJournalIdAndDateBetween(
            CompanyId companyId, JournalId journalId, LocalDate from, LocalDate to);

    boolean existsBySequenceNumberAndCompanyIdAndJournalId(String sequenceNumber, CompanyId companyId, JournalId journalId);
}
