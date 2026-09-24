package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.mapper.AccountingDataMapper;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.bradox.erp.accounting.service.domain.ports.output.sequence.SequenceGeneratorPort;
import com.bradox.erp.domain.core.AccountingDomainService;
import com.bradox.erp.domain.core.ValueObject.JournalEntryId;
import com.bradox.erp.domain.core.entity.JournalEntry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
class ReverseJournalEntryCommandHandler {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountingDomainService accountingDomainService;
    private final AccountingDataMapper mapper;
    private final SequenceGeneratorPort sequenceGeneratorPort;
    private final PeriodPostingGuard periodPostingGuard;
    private final PostJournalEntryCommandHandler postJournalEntryCommandHandler;

    ReverseJournalEntryCommandHandler(JournalEntryRepository journalEntryRepository,
                                      AccountingDomainService accountingDomainService,
                                      AccountingDataMapper mapper,
                                      SequenceGeneratorPort sequenceGeneratorPort,
                                      PeriodPostingGuard periodPostingGuard,
                                      PostJournalEntryCommandHandler postJournalEntryCommandHandler) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountingDomainService = accountingDomainService;
        this.mapper = mapper;
        this.sequenceGeneratorPort = sequenceGeneratorPort;
        this.periodPostingGuard = periodPostingGuard;
        this.postJournalEntryCommandHandler = postJournalEntryCommandHandler;
    }

    @Transactional
    ReverseJournalEntryResponse reverseJournalEntry(ReverseJournalEntryCommand command) {
        JournalEntry original = journalEntryRepository.findById(new JournalEntryId(command.getJournalEntryId()))
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found: " + command.getJournalEntryId()));
        LocalDateTime reversalDate = LocalDateTime.now();
        periodPostingGuard.assertDatePostable(original.getCompanyId(), reversalDate.toLocalDate());
        String reversalSequenceNumber = sequenceGeneratorPort.getNextSequenceNumber(
                original.getCompanyId(), original.getJournalId(), reversalDate.toLocalDate());
        JournalEntry reversal = accountingDomainService.createReversalEntry(original, command.getReason(), reversalSequenceNumber);
        reversal = journalEntryRepository.save(reversal);
        postJournalEntryCommandHandler.postJournalEntry(reversal.getId().getId());
        reversal = journalEntryRepository.findById(reversal.getId())
                .orElseThrow(() -> new IllegalArgumentException("Reversal entry not found after post"));
        return mapper.toReverseResponse(original, reversal, "Reversal entry created and posted successfully.");
    }
}
