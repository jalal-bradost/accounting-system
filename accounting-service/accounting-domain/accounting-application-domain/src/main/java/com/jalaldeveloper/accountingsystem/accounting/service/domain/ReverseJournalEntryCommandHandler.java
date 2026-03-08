package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.sequence.SequenceGeneratorPort;
import com.jalaldeveloper.accountingsystem.domain.core.AccountingDomainService;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import org.springframework.stereotype.Component;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
class ReverseJournalEntryCommandHandler {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountingDomainService accountingDomainService;
    private final AccountingDataMapper mapper;
    private final SequenceGeneratorPort sequenceGeneratorPort;

    ReverseJournalEntryCommandHandler(JournalEntryRepository journalEntryRepository,
                                      AccountingDomainService accountingDomainService,
                                      AccountingDataMapper mapper,
                                      SequenceGeneratorPort sequenceGeneratorPort) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountingDomainService = accountingDomainService;
        this.mapper = mapper;
        this.sequenceGeneratorPort = sequenceGeneratorPort;
    }

    @Transactional
    ReverseJournalEntryResponse reverseJournalEntry(ReverseJournalEntryCommand command) {
        JournalEntry original = journalEntryRepository.findById(new JournalEntryId(command.getJournalEntryId()))
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found: " + command.getJournalEntryId()));
        LocalDate reversalDate = LocalDate.now();
        String reversalSequenceNumber = sequenceGeneratorPort.getNextSequenceNumber(
                original.getCompanyId(), original.getJournalId(), reversalDate);
        JournalEntry reversal = accountingDomainService.createReversalEntry(original, command.getReason(), reversalSequenceNumber);
        reversal = journalEntryRepository.save(reversal);
        accountingDomainService.postJournalEntry(reversal);
        reversal = journalEntryRepository.save(reversal);
        return mapper.toReverseResponse(original, reversal, "Reversal entry created and posted successfully.");
    }
}
