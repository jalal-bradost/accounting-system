package com.jalaldeveloper.accountingsystem.hr.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.payroll.PayrollApi.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.PayrollApplicationService;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.PayRunApplicationService;
import com.jalaldeveloper.accountingsystem.platform.application.dto.PageResponse;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/payroll", produces = "application/json")
public class PayrollController {

    private final PayrollApplicationService service;
    private final PayRunApplicationService payRunService;

    public PayrollController(PayrollApplicationService service, PayRunApplicationService payRunService) {
        this.service = service;
        this.payRunService = payRunService;
    }

    @GetMapping("/working-schedules")
    @RequiresPermission("payroll.read")
    public ResponseEntity<List<WorkingScheduleSummaryResponse>> listSchedules(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(service.listWorkingSchedules(companyId));
    }

    @GetMapping("/working-schedules/{id}")
    @RequiresPermission("payroll.read")
    public ResponseEntity<WorkingScheduleResponse> getSchedule(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getWorkingSchedule(id));
    }

    @PostMapping("/working-schedules")
    @RequiresPermission("payroll.write")
    public ResponseEntity<WorkingScheduleResponse> createSchedule(@Valid @RequestBody SaveWorkingScheduleCommand cmd) {
        return ResponseEntity.ok(service.createWorkingSchedule(cmd));
    }

    @PutMapping("/working-schedules/{id}")
    @RequiresPermission("payroll.write")
    public ResponseEntity<WorkingScheduleResponse> updateSchedule(@PathVariable UUID id,
                                                                    @Valid @RequestBody SaveWorkingScheduleCommand cmd) {
        return ResponseEntity.ok(service.updateWorkingSchedule(id, cmd));
    }

    @GetMapping("/employee-types")
    @RequiresPermission("payroll.read")
    public ResponseEntity<List<EmployeeTypeResponse>> listEmployeeTypes(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(service.listEmployeeTypes(companyId));
    }

    @PostMapping("/employee-types")
    @RequiresPermission("payroll.write")
    public ResponseEntity<EmployeeTypeResponse> createEmployeeType(@Valid @RequestBody SaveEmployeeTypeCommand cmd) {
        return ResponseEntity.ok(service.createEmployeeType(cmd));
    }

    @PutMapping("/employee-types/{id}")
    @RequiresPermission("payroll.write")
    public ResponseEntity<EmployeeTypeResponse> updateEmployeeType(@PathVariable UUID id,
                                                                     @Valid @RequestBody SaveEmployeeTypeCommand cmd) {
        return ResponseEntity.ok(service.updateEmployeeType(id, cmd));
    }

    @GetMapping("/structure-types")
    @RequiresPermission("payroll.read")
    public ResponseEntity<List<StructureTypeResponse>> listStructureTypes(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(service.listStructureTypes(companyId));
    }

    @PostMapping("/structure-types")
    @RequiresPermission("payroll.write")
    public ResponseEntity<StructureTypeResponse> createStructureType(@Valid @RequestBody SaveStructureTypeCommand cmd) {
        return ResponseEntity.ok(service.createStructureType(cmd));
    }

    @PutMapping("/structure-types/{id}")
    @RequiresPermission("payroll.write")
    public ResponseEntity<StructureTypeResponse> updateStructureType(@PathVariable UUID id,
                                                                       @Valid @RequestBody SaveStructureTypeCommand cmd) {
        return ResponseEntity.ok(service.updateStructureType(id, cmd));
    }

    @GetMapping("/structures")
    @RequiresPermission("payroll.read")
    public ResponseEntity<List<StructureSummaryResponse>> listStructures(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(service.listStructures(companyId));
    }

    @GetMapping("/structures/{id}")
    @RequiresPermission("payroll.read")
    public ResponseEntity<StructureResponse> getStructure(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getStructure(id));
    }

    @PostMapping("/structures")
    @RequiresPermission("payroll.write")
    public ResponseEntity<StructureResponse> createStructure(@Valid @RequestBody SaveStructureCommand cmd) {
        return ResponseEntity.ok(service.createStructure(cmd));
    }

    @PutMapping("/structures/{id}")
    @RequiresPermission("payroll.write")
    public ResponseEntity<StructureResponse> updateStructure(@PathVariable UUID id,
                                                               @Valid @RequestBody SaveStructureCommand cmd) {
        return ResponseEntity.ok(service.updateStructure(id, cmd));
    }

    @GetMapping("/contracts")
    @RequiresPermission("payroll.read")
    public ResponseEntity<PageResponse<ContractSummaryResponse>> listContracts(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size) {
        var result = service.listContracts(companyId, employeeId, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result, Function.identity()));
    }

    @GetMapping("/contracts/{id}")
    @RequiresPermission("payroll.read")
    public ResponseEntity<ContractResponse> getContract(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getContract(id));
    }

    @GetMapping("/contracts/by-employee/{employeeId}")
    @RequiresPermission("payroll.read")
    public ResponseEntity<ContractResponse> getActiveContract(@CurrentCompany CompanyId companyId,
                                                              @PathVariable UUID employeeId) {
        ContractResponse contract = service.getActiveContractForEmployee(companyId, employeeId);
        if (contract == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(contract);
    }

    @PostMapping("/contracts")
    @RequiresPermission("payroll.write")
    public ResponseEntity<ContractResponse> createContract(@Valid @RequestBody SaveContractCommand cmd) {
        return ResponseEntity.ok(service.createContract(cmd));
    }

    @PutMapping("/contracts/{id}")
    @RequiresPermission("payroll.write")
    public ResponseEntity<ContractResponse> updateContract(@PathVariable UUID id,
                                                           @RequestBody SaveContractCommand cmd) {
        return ResponseEntity.ok(service.updateContract(id, cmd));
    }

    @GetMapping("/contracts/{id}/payslip-preview")
    @RequiresPermission("payroll.read")
    public ResponseEntity<PayslipPreviewResponse> previewPayslip(@PathVariable UUID id) {
        return ResponseEntity.ok(service.previewPayslip(id));
    }

    @GetMapping("/payslips/{id}")
    @RequiresPermission("payroll.read")
    public ResponseEntity<PayslipResponse> getPayslip(@PathVariable UUID id) {
        return ResponseEntity.ok(payRunService.getPayslip(id));
    }
}
