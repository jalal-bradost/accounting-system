package com.bradox.delin.accounting.service.domain;

import com.bradox.delin.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.delin.accounting.service.domain.mapper.AccountingDataMapper;
import com.bradox.delin.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.bradox.delin.domain.core.AccountingDomainService;
import com.bradox.delin.domain.core.ValueObject.JournalEntryId;
import com.bradox.delin.domain.core.ValueObject.JournalEntryStatus;
import com.bradox.delin.domain.core.entity.JournalEntry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class PostJournalEntryCommandHandler {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountingDomainService accountingDomainService;
    private final AccountingDataMapper mapper;
    private final PeriodPostingGuard periodPostingGuard;

    PostJournalEntryCommandHandler(JournalEntryRepository journalEntryRepository,
                                  AccountingDomainService accountingDomainService,
                                  AccountingDataMapper mapper,
                                  PeriodPostingGuard periodPostingGuard) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountingDomainService = accountingDomainService;
        this.mapper = mapper;
        this.periodPostingGuard = periodPostingGuard;
    }

    @Transactional
    CreateJournalEntryResponse postJournalEntry(UUID journalEntryId) {
        JournalEntry entry = journalEntryRepository.findById(new JournalEntryId(journalEntryId))
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found: " + journalEntryId));
        if (entry.getStatus() == JournalEntryStatus.POSTED) {
            return mapper.journalEntryToCreateResponse(entry, "Journal entry already posted.");
        }
        periodPostingGuard.assertDatePostable(entry.getCompanyId(), entry.getDate().toLocalDate());
        accountingDomainService.postJournalEntry(entry);
        JournalEntry saved = journalEntryRepository.save(entry);
        return mapper.journalEntryToCreateResponse(saved, "Journal entry posted successfully.");
    }
}
