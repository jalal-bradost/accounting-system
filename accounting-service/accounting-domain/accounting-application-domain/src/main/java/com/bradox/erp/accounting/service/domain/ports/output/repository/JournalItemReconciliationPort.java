package com.bradox.erp.accounting.service.domain.ports.output.repository;

import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.core.ValueObject.JournalItemId;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Output port for setting/clearing reconciliation id on journal items.
 */
public interface JournalItemReconciliationPort {

    /** Set the same reconciliation id on all given items. */
    void setReconciliation(List<JournalItemId> itemIds, UUID reconciliationId);

    /** Clear reconciliation id on all given items. */
    void clearReconciliation(List<JournalItemId> itemIds);

    /** All journal item ids that share any of the given reconciliation ids. */
    List<UUID> findItemIdsByReconciliationIds(List<UUID> reconciliationIds);

    /** Snapshots of the given journal items (unknown ids are omitted). */
    List<ItemSnapshot> findItemsByIds(Collection<UUID> itemIds);

    /** Snapshots of all items belonging to the given journal entries. */
    List<ItemSnapshot> findItemsByEntryIds(Collection<UUID> journalEntryIds);

    /** Original entry id → id of its posted reversal entry, for the given originals that have one. */
    java.util.Map<UUID, UUID> findPostedReversalEntryIds(Collection<UUID> journalEntryIds);

    /**
     * @param partnerId effective partner (line partner, falling back to the entry partner)
     */
    record ItemSnapshot(
            UUID itemId,
            UUID journalEntryId,
            UUID companyId,
            UUID accountId,
            AccountType accountType,
            UUID partnerId,
            BigDecimal debit,
            BigDecimal credit,
            UUID reconciliationId,
            String entryStatus) {

        public BigDecimal balance() {
            BigDecimal d = debit != null ? debit : BigDecimal.ZERO;
            BigDecimal c = credit != null ? credit : BigDecimal.ZERO;
            return d.subtract(c);
        }

        public boolean isTrade() {
            return accountType == AccountType.RECEIVABLE || accountType == AccountType.PAYABLE;
        }
    }
}
