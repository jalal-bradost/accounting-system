package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
class JournalEntryApplicationServiceImpl implements JournalEntryApplicationService {

    private final CreateJournalEntryCommandHandler createJournalEntryCommandHandler;
    private final PostJournalEntryCommandHandler postJournalEntryCommandHandler;
    private final ReverseJournalEntryCommandHandler reverseJournalEntryCommandHandler;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountingDataMapper mapper;

    JournalEntryApplicationServiceImpl(CreateJournalEntryCommandHandler createJournalEntryCommandHandler,
                                       PostJournalEntryCommandHandler postJournalEntryCommandHandler,
                                       ReverseJournalEntryCommandHandler reverseJournalEntryCommandHandler,
                                       JournalEntryRepository journalEntryRepository,
                                       AccountingDataMapper mapper) {
        this.createJournalEntryCommandHandler = createJournalEntryCommandHandler;
        this.postJournalEntryCommandHandler = postJournalEntryCommandHandler;
        this.reverseJournalEntryCommandHandler = reverseJournalEntryCommandHandler;
        this.journalEntryRepository = journalEntryRepository;
        this.mapper = mapper;
    }

    @Override
    public CreateJournalEntryResponse createJournalEntry(CreateJournalEntryCommand command) {
        return createJournalEntryCommandHandler.createJournalEntry(command);
    }

    @Override
    public CreateJournalEntryResponse postJournalEntry(UUID journalEntryId) {
        return postJournalEntryCommandHandler.postJournalEntry(journalEntryId);
    }

    @Override
    public ReverseJournalEntryResponse reverseJournalEntry(ReverseJournalEntryCommand command) {
        return reverseJournalEntryCommandHandler.reverseJournalEntry(command);
    }

    @Override
    public JournalEntryResponse getJournalEntry(UUID journalEntryId) {
        return journalEntryRepository.findById(new JournalEntryId(journalEntryId))
                .map(mapper::journalEntryToJournalEntryResponse)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found: " + journalEntryId));
    }
}
