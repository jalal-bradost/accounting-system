package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.repository.LedgerActivityRepository;
import com.bradox.erp.dataaccess.repository.JournalEntryJpaRepository;
import com.bradox.erp.domain.valueobject.CompanyId;
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
