package com.bradox.erp.accounting.service.domain.ports.output;

import java.util.Optional;
import java.util.UUID;

/**
 * Finds the business document (invoice, bill, payment, stock valuation, …) that a journal entry
 * was posted for. Such entries must be undone through their document, not reversed directly.
 */
public interface JournalEntryOwnershipPort {

    /** Human-readable owner such as {@code customer invoice INV/2026/00048}, or empty when unowned. */
    Optional<String> findOwner(UUID journalEntryId);
}
