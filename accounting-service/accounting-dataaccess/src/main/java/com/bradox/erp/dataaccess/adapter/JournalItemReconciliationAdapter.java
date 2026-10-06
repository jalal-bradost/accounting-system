package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort;
import com.bradox.erp.dataaccess.repository.JournalItemJpaRepository;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.core.ValueObject.JournalItemId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JournalItemReconciliationAdapter implements JournalItemReconciliationPort {

    private final JournalItemJpaRepository journalItemJpaRepository;

    public JournalItemReconciliationAdapter(JournalItemJpaRepository journalItemJpaRepository) {
        this.journalItemJpaRepository = journalItemJpaRepository;
    }

    @Override
    @Transactional
    public void setReconciliation(List<JournalItemId> itemIds, UUID reconciliationId) {
        if (itemIds == null || itemIds.isEmpty()) return;
        List<UUID> ids = itemIds.stream().map(JournalItemId::getId).collect(Collectors.toList());
        journalItemJpaRepository.setReconciliationId(ids, reconciliationId);
    }

    @Override
    @Transactional
    public void clearReconciliation(List<JournalItemId> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return;
        List<UUID> ids = itemIds.stream().map(JournalItemId::getId).collect(Collectors.toList());
        journalItemJpaRepository.clearReconciliationId(ids);
    }

    @Override
    public List<UUID> findItemIdsByReconciliationIds(List<UUID> reconciliationIds) {
        if (reconciliationIds == null || reconciliationIds.isEmpty()) {
            return List.of();
        }
        return journalItemJpaRepository.findIdsByReconciliationIdIn(reconciliationIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemSnapshot> findItemsByIds(Collection<UUID> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return List.of();
        return journalItemJpaRepository.findSnapshotsByIds(itemIds).stream().map(this::toSnapshot).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemSnapshot> findItemsByEntryIds(Collection<UUID> journalEntryIds) {
        if (journalEntryIds == null || journalEntryIds.isEmpty()) return List.of();
        return journalItemJpaRepository.findSnapshotsByEntryIds(journalEntryIds).stream().map(this::toSnapshot).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<UUID, UUID> findPostedReversalEntryIds(Collection<UUID> journalEntryIds) {
        if (journalEntryIds == null || journalEntryIds.isEmpty()) return java.util.Map.of();
        java.util.Map<UUID, UUID> out = new java.util.HashMap<>();
        for (Object[] r : journalItemJpaRepository.findPostedReversalsOf(journalEntryIds)) {
            out.putIfAbsent((UUID) r[0], (UUID) r[1]);
        }
        return out;
    }

    private ItemSnapshot toSnapshot(Object[] r) {
        return new ItemSnapshot(
                (UUID) r[0],
                (UUID) r[1],
                (UUID) r[2],
                (UUID) r[3],
                (AccountType) r[4],
                (UUID) r[5],
                (BigDecimal) r[6],
                (BigDecimal) r[7],
                (UUID) r[8],
                r[9] != null ? r[9].toString() : null);
    }
}
