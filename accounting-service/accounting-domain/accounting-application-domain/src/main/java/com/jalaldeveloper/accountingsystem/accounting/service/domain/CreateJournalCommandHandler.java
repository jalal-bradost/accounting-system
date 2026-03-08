package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalRepository;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Journal;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class CreateJournalCommandHandler {

    private final JournalRepository journalRepository;
    private final AccountingDataMapper mapper;

    CreateJournalCommandHandler(JournalRepository journalRepository, AccountingDataMapper mapper) {
        this.journalRepository = journalRepository;
        this.mapper = mapper;
    }

    @Transactional
    CreateJournalResponse createJournal(CreateJournalCommand command) {
        if (journalRepository.existsByCompanyIdAndCode(new CompanyId(command.getCompanyId()), command.getCode())) {
            throw new IllegalArgumentException("Journal already exists with code: " + command.getCode());
        }
        UUID id = UUID.randomUUID();
        Journal journal = mapper.createJournalCommandToJournal(command, id);
        Journal saved = journalRepository.save(journal);
        return mapper.journalToCreateJournalResponse(saved, "Journal created successfully.");
    }
}
