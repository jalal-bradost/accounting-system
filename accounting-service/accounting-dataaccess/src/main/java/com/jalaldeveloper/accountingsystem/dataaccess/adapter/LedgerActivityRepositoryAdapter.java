package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.LedgerActivityRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalEntryJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
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
