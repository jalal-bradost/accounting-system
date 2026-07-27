package com.jalaldeveloper.accountingsystem.hr.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.CreateEmployeeCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.EmployeeResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.EmployeeSummaryResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.UpdateEmployeeCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.EmployeeApplicationService;
import com.jalaldeveloper.accountingsystem.platform.application.dto.PageResponse;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/hr/employees", produces = "application/json")
public class EmployeeController {

    private final EmployeeApplicationService service;

    public EmployeeController(EmployeeApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("hr.employee.write")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeCommand cmd) {
        return ResponseEntity.ok(service.create(cmd));
    }

    @GetMapping("/me")
    @RequiresPermission(value = {"hr.employee.self.read", "hr.employee.read"}, op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<EmployeeResponse> getMe() {
        return ResponseEntity.ok(service.getMe());
    }

    @GetMapping("/{id}")
    @RequiresPermission("hr.employee.read")
    public ResponseEntity<EmployeeResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequiresPermission("hr.employee.write")
    public ResponseEntity<EmployeeResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateEmployeeCommand cmd) {
        return ResponseEntity.ok(service.update(id, cmd));
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("hr.employee.archive")
    public ResponseEntity<EmployeeResponse> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(service.archive(id));
    }

    @PostMapping("/{id}/unarchive")
    @RequiresPermission("hr.employee.archive")
    public ResponseEntity<EmployeeResponse> unarchive(@PathVariable UUID id) {
        return ResponseEntity.ok(service.unarchive(id));
    }

    @GetMapping
    @RequiresPermission("hr.employee.read")
    public ResponseEntity<PageResponse<EmployeeSummaryResponse>> list(@CurrentCompany CompanyId companyId,
                                                                      @RequestParam(required = false) String q,
                                                                      @RequestParam(required = false) UUID departmentId,
                                                                      @RequestParam(defaultValue = "false") boolean includeArchived,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "50") int size) {
        var result = service.search(companyId, q, departmentId, includeArchived, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result, Function.identity()));
    }

    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    @RequiresPermission("hr.employee.write")
    public ResponseEntity<EmployeeResponse> uploadImage(@PathVariable UUID id,
                                                        @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadImage(id, file));
    }

    @DeleteMapping("/{id}/image")
    @RequiresPermission("hr.employee.write")
    public ResponseEntity<EmployeeResponse> deleteImage(@PathVariable UUID id) {
        return ResponseEntity.ok(service.deleteImage(id));
    }
}
