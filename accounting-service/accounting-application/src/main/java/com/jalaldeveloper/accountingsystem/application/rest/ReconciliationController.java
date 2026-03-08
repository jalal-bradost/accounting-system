package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReconciliationApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/journal-items", produces = "application/json")
public class ReconciliationController {

    private final ReconciliationApplicationService reconciliationApplicationService;

    public ReconciliationController(ReconciliationApplicationService reconciliationApplicationService) {
        this.reconciliationApplicationService = reconciliationApplicationService;
    }

    @PostMapping("/reconcile")
    public ResponseEntity<Void> reconcile(@Valid @RequestBody ReconcileRequest request) {
        reconciliationApplicationService.reconcile(
                new ReconciliationApplicationService.ReconcileCommand(
                        request.getJournalItemIds(),
                        request.getReconciliationId()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unreconcile")
    public ResponseEntity<Void> unreconcile(@Valid @RequestBody UnreconcileRequest request) {
        reconciliationApplicationService.unreconcile(
                new ReconciliationApplicationService.UnreconcileCommand(request.getJournalItemIds()));
        return ResponseEntity.noContent().build();
    }

    public static class ReconcileRequest {
        @NotEmpty
        private List<UUID> journalItemIds;
        @NotNull
        private UUID reconciliationId;
        public List<UUID> getJournalItemIds() { return journalItemIds; }
        public void setJournalItemIds(List<UUID> journalItemIds) { this.journalItemIds = journalItemIds; }
        public UUID getReconciliationId() { return reconciliationId; }
        public void setReconciliationId(UUID reconciliationId) { this.reconciliationId = reconciliationId; }
    }

    public static class UnreconcileRequest {
        @NotEmpty
        private List<UUID> journalItemIds;
        public List<UUID> getJournalItemIds() { return journalItemIds; }
        public void setJournalItemIds(List<UUID> journalItemIds) { this.journalItemIds = journalItemIds; }
    }
}
