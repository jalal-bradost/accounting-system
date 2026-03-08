package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryResponse;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

public interface JournalEntryApplicationService {

    CreateJournalEntryResponse createJournalEntry(@Valid CreateJournalEntryCommand command);

    CreateJournalEntryResponse postJournalEntry(UUID journalEntryId);

    ReverseJournalEntryResponse reverseJournalEntry(@Valid ReverseJournalEntryCommand command);

    JournalEntryResponse getJournalEntry(UUID journalEntryId);

    List<JournalEntryResponse> listJournalEntriesByCompany(UUID companyId);
}
