package com.jalaldeveloper.accountingsystem.hr.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.timeoff.TimeOffApi.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.TimeOffApplicationService;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/hr/time-off", produces = "application/json")
public class TimeOffController {

    private final TimeOffApplicationService service;

    public TimeOffController(TimeOffApplicationService service) {
        this.service = service;
    }

    @GetMapping("/types")
    @RequiresPermission(value = {"hr.time-off.read", "hr.time-off.self.read"}, op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<List<TimeOffTypeResponse>> listTypes(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(service.listTypes(companyId));
    }

    @PostMapping("/types")
    @RequiresPermission("hr.time-off.write")
    public ResponseEntity<TimeOffTypeResponse> createType(@Valid @RequestBody SaveTimeOffTypeCommand cmd) {
        return ResponseEntity.ok(service.createType(cmd));
    }

    @GetMapping("/allocations")
    @RequiresPermission(value = {"hr.time-off.read", "hr.time-off.self.read"}, op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<List<AllocationResponse>> listAllocations(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) UUID employeeId) {
        return ResponseEntity.ok(service.listAllocations(companyId, employeeId));
    }

    @PostMapping("/allocations")
    @RequiresPermission("hr.time-off.write")
    public ResponseEntity<AllocationResponse> createAllocation(@Valid @RequestBody SaveAllocationCommand cmd) {
        return ResponseEntity.ok(service.createAllocation(cmd));
    }

    @PostMapping("/allocations/{id}/approve")
    @RequiresPermission("hr.time-off.approve")
    public ResponseEntity<AllocationResponse> approveAllocation(@PathVariable UUID id) {
        return ResponseEntity.ok(service.approveAllocation(id));
    }

    @PostMapping("/allocations/{id}/refuse")
    @RequiresPermission("hr.time-off.approve")
    public ResponseEntity<AllocationResponse> refuseAllocation(@PathVariable UUID id) {
        return ResponseEntity.ok(service.refuseAllocation(id));
    }

    @GetMapping("/requests")
    @RequiresPermission(value = {"hr.time-off.read", "hr.time-off.self.read"}, op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<List<LeaveRequestResponse>> listRequests(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "2026") int year) {
        return ResponseEntity.ok(service.listRequests(companyId, employeeId, state, year));
    }

    @PostMapping("/requests")
    @RequiresPermission(value = {"hr.time-off.write", "hr.time-off.self.write"}, op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<LeaveRequestResponse> createRequest(@Valid @RequestBody SaveLeaveRequestCommand cmd) {
        return ResponseEntity.ok(service.createRequest(cmd));
    }

    @PostMapping("/requests/{id}/approve")
    @RequiresPermission("hr.time-off.approve")
    public ResponseEntity<LeaveRequestResponse> approveRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(service.approveRequest(id));
    }

    @PostMapping("/requests/{id}/refuse")
    @RequiresPermission("hr.time-off.approve")
    public ResponseEntity<LeaveRequestResponse> refuseRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(service.refuseRequest(id));
    }

    @GetMapping("/dashboard")
    @RequiresPermission(value = {"hr.time-off.read", "hr.time-off.self.read"}, op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<DashboardResponse> dashboard(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(defaultValue = "2026") int year) {
        return ResponseEntity.ok(service.dashboard(companyId, employeeId, year));
    }

    @GetMapping("/team-summary")
    @RequiresPermission("hr.time-off.read")
    public ResponseEntity<TeamSummaryResponse> teamSummary(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) UUID managerId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        return ResponseEntity.ok(service.teamSummary(companyId, managerId, date));
    }
}
