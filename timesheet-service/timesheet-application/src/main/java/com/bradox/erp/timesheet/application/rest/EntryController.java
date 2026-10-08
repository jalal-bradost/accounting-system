package com.bradox.erp.timesheet.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.timesheet.service.domain.dto.CopyEntriesCommand;
import com.bradox.erp.timesheet.service.domain.dto.EntryCommand;
import com.bradox.erp.timesheet.service.domain.dto.EntryListResponse;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.EntryApplicationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/timesheet/entries", produces = "application/json")
public class EntryController {

    private final EntryApplicationService entries;

    public EntryController(EntryApplicationService entries) {
        this.entries = entries;
    }

    @GetMapping
    @RequiresPermission("tsh.entry.own")
    public EntryListResponse list(@CurrentCompany CompanyId companyId,
                                  @RequestParam(required = false) UUID employeeId,
                                  @RequestParam(defaultValue = "false") boolean all,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) UUID projectId,
                                  @RequestParam(required = false) UUID taskId,
                                  @RequestParam(required = false) Boolean billable) {
        return entries.list(companyId, employeeId, all, from, to, projectId, taskId, billable);
    }

    @GetMapping("/by-record")
    @RequiresPermission("tsh.entry.own")
    public EntryListResponse byRecord(@CurrentCompany CompanyId companyId, @RequestParam String model,
                                      @RequestParam UUID recordId) {
        return entries.byRecord(companyId, model, recordId);
    }

    @GetMapping("/{id}")
    @RequiresPermission("tsh.entry.own")
    public EntryResponse get(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return entries.get(companyId, id);
    }

    @PostMapping
    @RequiresPermission("tsh.entry.own")
    public EntryResponse log(@CurrentCompany CompanyId companyId, @Valid @RequestBody EntryCommand command) {
        return entries.log(companyId, command);
    }

    @PutMapping("/{id}")
    @RequiresPermission("tsh.entry.own")
    public EntryResponse update(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                @Valid @RequestBody EntryCommand command) {
        return entries.update(companyId, id, command);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("tsh.entry.own")
    public ResponseEntity<Void> delete(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        entries.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/copy")
    @RequiresPermission("tsh.entry.own")
    public List<EntryResponse> copy(@CurrentCompany CompanyId companyId, @Valid @RequestBody CopyEntriesCommand command) {
        return entries.copy(companyId, command);
    }
}
