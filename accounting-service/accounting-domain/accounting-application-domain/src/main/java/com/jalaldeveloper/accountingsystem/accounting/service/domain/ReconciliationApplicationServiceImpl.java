package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReconciliationApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalItemId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReconciliationApplicationServiceImpl implements ReconciliationApplicationService {

    private final JournalItemReconciliationPort journalItemReconciliationPort;

    public ReconciliationApplicationServiceImpl(JournalItemReconciliationPort journalItemReconciliationPort) {
        this.journalItemReconciliationPort = journalItemReconciliationPort;
    }

    @Override
    public void reconcile(ReconcileCommand command) {
        if (command.journalItemIds() == null || command.journalItemIds().isEmpty()) return;
        if (command.reconciliationId() == null) {
            throw new IllegalArgumentException("reconciliationId is required");
        }
        List<JournalItemId> itemIds = command.journalItemIds().stream()
                .map(JournalItemId::new)
                .collect(Collectors.toList());
        journalItemReconciliationPort.setReconciliation(itemIds, command.reconciliationId());
    }

    @Override
    public void unreconcile(UnreconcileCommand command) {
        if (command.journalItemIds() == null || command.journalItemIds().isEmpty()) return;
        List<JournalItemId> itemIds = command.journalItemIds().stream()
                .map(JournalItemId::new)
                .collect(Collectors.toList());
        journalItemReconciliationPort.clearReconciliation(itemIds);
    }
}
