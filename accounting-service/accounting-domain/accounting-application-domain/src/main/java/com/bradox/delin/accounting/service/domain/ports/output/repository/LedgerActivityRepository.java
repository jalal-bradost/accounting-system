package com.bradox.delin.accounting.service.domain.ports.output.repository;

import com.bradox.delin.domain.valueobject.CompanyId;

/** Read-only checks about whether a company already has ledger activity. */
public interface LedgerActivityRepository {

    /** True when the company has at least one journal entry (draft or posted). */
    boolean hasJournalEntries(CompanyId companyId);
}
