package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;

import java.util.UUID;

/**
 * Output port for resolving accounting reference data (accounts, journals) without
 * exposing persistence types. Implemented in {@code accounting-dataaccess}; consumed
 * by other modules (e.g. purchase) via the application-domain port only.
 */
public interface AccountingReferenceLookupPort {

    UUID resolveAccountIdByCode(UUID companyId, String code);

    UUID resolveLiquidityAccountIdForJournal(UUID companyId, UUID journalId);

    UUID resolveJournalIdByType(UUID companyId, JournalType journalType);

    JournalType resolveJournalType(UUID companyId, UUID journalId);
}
