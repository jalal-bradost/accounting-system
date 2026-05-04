package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/journal-entries", produces = "application/json")
public class JournalEntryController {

    private final JournalEntryApplicationService journalEntryApplicationService;

    public JournalEntryController(JournalEntryApplicationService journalEntryApplicationService) {
        this.journalEntryApplicationService = journalEntryApplicationService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalEntryResponse> getJournalEntry(@PathVariable UUID id) {
        return ResponseEntity.ok(journalEntryApplicationService.getJournalEntry(id));
    }

    @GetMapping
    public ResponseEntity<List<JournalEntryResponse>> listJournalEntries(@RequestParam UUID companyId) {
        return ResponseEntity.ok(journalEntryApplicationService.listJournalEntriesByCompany(companyId));
    }

    @PostMapping
    public ResponseEntity<CreateJournalEntryResponse> createJournalEntry(@Valid @RequestBody CreateJournalEntryCommand command) {
        CreateJournalEntryResponse response = journalEntryApplicationService.createJournalEntry(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<CreateJournalEntryResponse> postJournalEntry(@PathVariable UUID id) {
        CreateJournalEntryResponse response = journalEntryApplicationService.postJournalEntry(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<ReverseJournalEntryResponse> reverseJournalEntry(@PathVariable UUID id,
                                                                          @RequestBody(required = false) ReverseJournalEntryRequest body) {
        String reason = body != null && body.getReason() != null && !body.getReason().isBlank()
                ? body.getReason()
                : "Reversal";
        ReverseJournalEntryCommand command = new ReverseJournalEntryCommand(id, reason);
        ReverseJournalEntryResponse response = journalEntryApplicationService.reverseJournalEntry(command);
        return ResponseEntity.ok(response);
    }
}
