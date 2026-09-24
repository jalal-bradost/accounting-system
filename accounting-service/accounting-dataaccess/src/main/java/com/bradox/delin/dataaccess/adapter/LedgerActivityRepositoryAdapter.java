package com.bradox.delin.dataaccess.adapter;

import com.bradox.delin.accounting.service.domain.ports.output.repository.LedgerActivityRepository;
import com.bradox.delin.dataaccess.repository.JournalEntryJpaRepository;
import com.bradox.delin.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

@Component
public class LedgerActivityRepositoryAdapter implements LedgerActivityRepository {

    private final JournalEntryJpaRepository journalEntryJpaRepository;

    public LedgerActivityRepositoryAdapter(JournalEntryJpaRepository journalEntryJpaRepository) {
        this.journalEntryJpaRepository = journalEntryJpaRepository;
    }

    @Override
    public boolean hasJournalEntries(CompanyId companyId) {
        return journalEntryJpaRepository.existsByCompanyId(companyId.getId());
    }
}
