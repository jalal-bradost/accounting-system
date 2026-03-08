package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalItem;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
class CreateJournalEntryCommandHandler {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountingDataMapper mapper;

    CreateJournalEntryCommandHandler(JournalEntryRepository journalEntryRepository, AccountingDataMapper mapper) {
        this.journalEntryRepository = journalEntryRepository;
        this.mapper = mapper;
    }

    @Transactional
    CreateJournalEntryResponse createJournalEntry(CreateJournalEntryCommand command) {
        UUID journalEntryId = UUID.randomUUID();
        List<JournalItem> items = mapper.journalItemCommandsToDomain(command.getItems());
        JournalEntry entry = mapper.createJournalEntryCommandToJournalEntry(command, journalEntryId, items);
        JournalEntry saved = journalEntryRepository.save(entry);
        return mapper.journalEntryToCreateResponse(saved, "Journal entry created successfully.");
    }
}
