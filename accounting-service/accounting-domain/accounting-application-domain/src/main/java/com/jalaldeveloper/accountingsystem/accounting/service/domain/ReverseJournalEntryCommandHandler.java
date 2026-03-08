package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.domain.core.AccountingDomainService;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ReverseJournalEntryCommandHandler {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountingDomainService accountingDomainService;
    private final AccountingDataMapper mapper;

    ReverseJournalEntryCommandHandler(JournalEntryRepository journalEntryRepository,
                                      AccountingDomainService accountingDomainService,
                                      AccountingDataMapper mapper) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountingDomainService = accountingDomainService;
        this.mapper = mapper;
    }

    @Transactional
    ReverseJournalEntryResponse reverseJournalEntry(ReverseJournalEntryCommand command) {
        JournalEntry original = journalEntryRepository.findById(new JournalEntryId(command.getJournalEntryId()))
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found: " + command.getJournalEntryId()));
        JournalEntry reversal = accountingDomainService.createReversalEntry(original, command.getReason());
        reversal = journalEntryRepository.save(reversal);
        accountingDomainService.postJournalEntry(reversal);
        reversal = journalEntryRepository.save(reversal);
        return mapper.toReverseResponse(original, reversal, "Reversal entry created and posted successfully.");
    }
}
