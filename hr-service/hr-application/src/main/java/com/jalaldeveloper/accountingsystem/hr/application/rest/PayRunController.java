package com.jalaldeveloper.accountingsystem.hr.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.payroll.PayrollApi.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.PayRunApplicationService;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/payroll/runs", produces = "application/json")
public class PayRunController {

    private final PayRunApplicationService service;

    public PayRunController(PayRunApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("payroll.write")
    public ResponseEntity<PayRunResponse> create(@Valid @RequestBody CreatePayRunCommand cmd) {
        return ResponseEntity.ok(service.createRun(cmd));
    }

    @GetMapping
    @RequiresPermission("payroll.read")
    public ResponseEntity<List<PayRunSummaryResponse>> list(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(service.listRuns(companyId));
    }

    @GetMapping("/{id}")
    @RequiresPermission("payroll.read")
    public ResponseEntity<PayRunResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getRun(id));
    }

    @PostMapping("/{id}/compute")
    @RequiresPermission("payroll.write")
    public ResponseEntity<PayRunResponse> compute(@PathVariable UUID id) {
        return ResponseEntity.ok(service.computeRun(id));
    }

    @PostMapping("/{id}/post")
    @RequiresPermission("payroll.post")
    public ResponseEntity<PayRunResponse> post(@PathVariable UUID id) {
        return ResponseEntity.ok(service.postRun(id));
    }

    @PostMapping("/{id}/pay")
    @RequiresPermission("payroll.pay")
    public ResponseEntity<PayRunResponse> pay(@PathVariable UUID id, @Valid @RequestBody PayRunCommand cmd) {
        return ResponseEntity.ok(service.payRun(id, cmd));
    }
}
