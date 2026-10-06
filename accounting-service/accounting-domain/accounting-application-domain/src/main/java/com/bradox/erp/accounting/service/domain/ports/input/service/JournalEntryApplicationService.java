package com.bradox.erp.accounting.service.domain.ports.input.service;

import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryResponse;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface JournalEntryApplicationService {

    CreateJournalEntryResponse createJournalEntry(@Valid CreateJournalEntryCommand command);

    CreateJournalEntryResponse postJournalEntry(UUID journalEntryId);

    ReverseJournalEntryResponse reverseJournalEntry(@Valid ReverseJournalEntryCommand command);

    /** User-initiated reversal; refuses entries that belong to a business document. */
    ReverseJournalEntryResponse reverseManualJournalEntry(@Valid ReverseJournalEntryCommand command);

    JournalEntryResponse getJournalEntry(UUID journalEntryId);

    List<JournalEntryResponse> listJournalEntriesByCompany(UUID companyId);

    Page<JournalEntryResponse> searchJournalEntries(UUID companyId, Pageable pageable);
}
