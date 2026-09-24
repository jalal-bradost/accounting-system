package com.bradox.delin.accounting.service.domain.ports.output.repository;

import com.bradox.delin.domain.core.ValueObject.JournalItemId;

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

    /** All journal item ids that share any of the given reconciliation ids. */
    List<UUID> findItemIdsByReconciliationIds(List<UUID> reconciliationIds);
}
