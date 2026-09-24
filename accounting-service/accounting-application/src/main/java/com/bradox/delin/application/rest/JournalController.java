package com.bradox.delin.application.rest;

import com.bradox.delin.accounting.service.domain.create.CreateJournalCommand;
import com.bradox.delin.accounting.service.domain.create.CreateJournalResponse;
import com.bradox.delin.accounting.service.domain.create.JournalResponse;
import com.bradox.delin.accounting.service.domain.ports.input.service.JournalApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/journals", produces = "application/json")
public class JournalController {

    private final JournalApplicationService journalApplicationService;

    public JournalController(JournalApplicationService journalApplicationService) {
        this.journalApplicationService = journalApplicationService;
    }

    @PostMapping
    public ResponseEntity<CreateJournalResponse> createJournal(@Valid @RequestBody CreateJournalCommand command) {
        CreateJournalResponse response = journalApplicationService.createJournal(command);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalResponse> getJournal(@PathVariable UUID id) {
        return ResponseEntity.ok(journalApplicationService.getJournal(id));
    }

    @GetMapping
    public ResponseEntity<List<JournalResponse>> listJournals(@RequestParam UUID companyId) {
        return ResponseEntity.ok(journalApplicationService.listJournalsByCompany(companyId));
    }
}
