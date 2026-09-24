package com.bradox.delin.dataaccess.adapter;

import com.bradox.delin.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort;
import com.bradox.delin.dataaccess.repository.JournalItemJpaRepository;
import com.bradox.delin.domain.core.ValueObject.JournalItemId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
}
