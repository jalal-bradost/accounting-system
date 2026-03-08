package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalItemId;

import java.util.List;
import java.util.UUID;

/**
 * Output port for setting/clearing reconciliation id on journal items (stub).
 */
public interface JournalItemReconciliationPort {

    /** Set the same reconciliation id on all given items. */
    void setReconciliation(List<JournalItemId> itemIds, UUID reconciliationId);

    /** Clear reconciliation id on all given items. */
    void clearReconciliation(List<JournalItemId> itemIds);
}
