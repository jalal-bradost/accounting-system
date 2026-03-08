package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.domain.core.AccountingDomainService;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class PostJournalEntryCommandHandler {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountingDomainService accountingDomainService;
    private final AccountingDataMapper mapper;

    PostJournalEntryCommandHandler(JournalEntryRepository journalEntryRepository,
                                  AccountingDomainService accountingDomainService,
                                  AccountingDataMapper mapper) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountingDomainService = accountingDomainService;
        this.mapper = mapper;
    }

    @Transactional
    CreateJournalEntryResponse postJournalEntry(UUID journalEntryId) {
        JournalEntry entry = journalEntryRepository.findById(new JournalEntryId(journalEntryId))
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found: " + journalEntryId));
        accountingDomainService.postJournalEntry(entry);
        JournalEntry saved = journalEntryRepository.save(entry);
        return mapper.journalEntryToCreateResponse(saved, "Journal entry posted successfully.");
    }
}
