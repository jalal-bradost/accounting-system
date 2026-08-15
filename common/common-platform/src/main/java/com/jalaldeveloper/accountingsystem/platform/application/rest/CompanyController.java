package com.jalaldeveloper.accountingsystem.platform.application.rest;

import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.settings.CompanyApplicationService;
import com.jalaldeveloper.accountingsystem.platform.settings.CompanyResponse;
import com.jalaldeveloper.accountingsystem.platform.settings.CompanyWriteRequest;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/platform/companies", produces = "application/json")
public class CompanyController {

    private final CompanyApplicationService service;
    private final CompanyContext companyContext;

    public CompanyController(CompanyApplicationService service, CompanyContext companyContext) {
        this.service = service;
        this.companyContext = companyContext;
    }

    @GetMapping("/me")
    public ResponseEntity<CompanyResponse> me() {
        UUID id = companyContext.requireCompany().getId();
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    @RequiresPermission("platform.company.read")
    public ResponseEntity<List<CompanyResponse>> list() {
        UUID userId = companyContext.currentUser().map(u -> u.getId()).orElse(null);
        return ResponseEntity.ok(service.listForUser(userId));
    }

    @GetMapping("/{id}")
    @RequiresPermission("platform.company.read")
    public ResponseEntity<CompanyResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    @RequiresPermission("platform.company.write")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyWriteRequest body) {
        UUID creatorUserId = companyContext.currentUser().map(u -> u.getId()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body, creatorUserId));
    }

    @PutMapping("/{id}")
    @RequiresPermission("platform.company.write")
    public ResponseEntity<CompanyResponse> update(@PathVariable UUID id, @Valid @RequestBody CompanyWriteRequest body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @PatchMapping("/{id}/period-lock")
    @RequiresPermission("platform.company.write")
    public ResponseEntity<CompanyResponse> setPeriodLock(@PathVariable UUID id,
                                                         @Valid @RequestBody PeriodLockRequest body) {
        return ResponseEntity.ok(service.setPeriodLock(id, body.periodLockDate()));
    }

    /** {@code periodLockDate} may be {@code null} to clear the lock. */
    public record PeriodLockRequest(LocalDate periodLockDate) {}
}
