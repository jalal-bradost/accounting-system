package com.bradox.delin.domain.core;

import com.bradox.delin.domain.core.entity.JournalEntry;

public interface AccountingDomainService {

    /**
     * Orchestrates the posting of a Journal Entry.
     * Validates balance and business rules before changing state.
     */
    void postJournalEntry(JournalEntry journalEntry);

    /**
     * Creates a reversal (contra) entry. The new entry will have reversalSequenceNumber and link to original via reversalOfEntryId.
     */
    JournalEntry createReversalEntry(JournalEntry originalEntry, String reason, String reversalSequenceNumber);
}
