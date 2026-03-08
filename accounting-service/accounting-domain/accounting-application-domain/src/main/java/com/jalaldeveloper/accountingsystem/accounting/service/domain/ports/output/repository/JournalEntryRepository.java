package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

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
