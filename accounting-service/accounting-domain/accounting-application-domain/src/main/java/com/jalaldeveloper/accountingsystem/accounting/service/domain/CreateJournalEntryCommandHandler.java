package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.sequence.SequenceGeneratorPort;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
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
    private final SequenceGeneratorPort sequenceGeneratorPort;

    CreateJournalEntryCommandHandler(JournalEntryRepository journalEntryRepository,
                                    AccountingDataMapper mapper,
                                    SequenceGeneratorPort sequenceGeneratorPort) {
        this.journalEntryRepository = journalEntryRepository;
        this.mapper = mapper;
        this.sequenceGeneratorPort = sequenceGeneratorPort;
    }

    @Transactional
    CreateJournalEntryResponse createJournalEntry(CreateJournalEntryCommand command) {
        UUID journalEntryId = UUID.randomUUID();
        String sequenceNumber = sequenceGeneratorPort.getNextSequenceNumber(
                new CompanyId(command.getCompanyId()),
                new JournalId(command.getJournalId()),
                command.getDate());
        List<JournalItem> items = mapper.journalItemCommandsToDomain(command.getItems());
        JournalEntry entry = mapper.createJournalEntryCommandToJournalEntry(command, journalEntryId, items, sequenceNumber);
        JournalEntry saved = journalEntryRepository.save(entry);
        return mapper.journalEntryToCreateResponse(saved, "Journal entry created successfully.");
    }
}
