package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface ReconciliationApplicationService {

    /** Set the same reconciliation id on the given journal items. */
    void reconcile(@Valid ReconcileCommand command);

    /** Clear reconciliation id on the given journal items. */
    void unreconcile(@Valid UnreconcileCommand command);

    record ReconcileCommand(List<UUID> journalItemIds, UUID reconciliationId) {}
    record UnreconcileCommand(List<UUID> journalItemIds) {}
}
