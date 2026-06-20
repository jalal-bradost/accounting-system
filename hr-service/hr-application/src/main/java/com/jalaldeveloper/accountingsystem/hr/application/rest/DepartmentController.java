package com.jalaldeveloper.accountingsystem.hr.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.CreateDepartmentCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.DepartmentResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.DepartmentSummaryResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.UpdateDepartmentCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.DepartmentApplicationService;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/hr/departments", produces = "application/json")
public class DepartmentController {

    private final DepartmentApplicationService service;

    public DepartmentController(DepartmentApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("hr.department.write")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentCommand cmd) {
        return ResponseEntity.ok(service.create(cmd));
    }

    @GetMapping("/{id}")
    @RequiresPermission("hr.department.read")
    public ResponseEntity<DepartmentResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequiresPermission("hr.department.write")
    public ResponseEntity<DepartmentResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateDepartmentCommand cmd) {
        return ResponseEntity.ok(service.update(id, cmd));
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("hr.department.write")
    public ResponseEntity<DepartmentResponse> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(service.archive(id));
    }

    @PostMapping("/{id}/unarchive")
    @RequiresPermission("hr.department.write")
    public ResponseEntity<DepartmentResponse> unarchive(@PathVariable UUID id) {
        return ResponseEntity.ok(service.unarchive(id));
    }

    @GetMapping
    @RequiresPermission("hr.department.read")
    public ResponseEntity<List<DepartmentSummaryResponse>> list(@CurrentCompany CompanyId companyId,
                                                                @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(service.list(companyId, includeArchived));
    }

    @GetMapping("/{id}/employee-count")
    @RequiresPermission("hr.department.read")
    public ResponseEntity<Long> employeeCount(@PathVariable UUID id) {
        return ResponseEntity.ok(service.countEmployees(id));
    }
}
