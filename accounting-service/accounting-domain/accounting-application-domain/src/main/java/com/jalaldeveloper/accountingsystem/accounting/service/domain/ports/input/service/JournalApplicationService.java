package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalResponse;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

public interface JournalApplicationService {

    CreateJournalResponse createJournal(@Valid CreateJournalCommand command);

    JournalResponse getJournal(UUID journalId);

    List<JournalResponse> listJournalsByCompany(UUID companyId);
}
