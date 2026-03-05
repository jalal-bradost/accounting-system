package com.jalaldeveloper.accountingsystem.domain.core;

import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;

public interface AccountingDomainService {

    /**
     * Orchestrates the posting of a Journal Entry.
     * Validates balance and business rules before changing state.
     */
    void postJournalEntry(JournalEntry journalEntry);

    /**
     * Logic for reversing an entry (Contra Entry)
     */
    JournalEntry createReversalEntry(JournalEntry originalEntry, String reason);
}
