package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalItemJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalItemId;
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
}
