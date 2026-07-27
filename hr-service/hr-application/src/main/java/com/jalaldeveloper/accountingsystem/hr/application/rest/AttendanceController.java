package com.jalaldeveloper.accountingsystem.hr.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.AttendanceResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.AttendanceSummaryResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.BulkCreateAttendanceCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.CreateAttendanceCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.GenerateAttendanceFromScheduleCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.UpdateAttendanceCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.AttendanceApplicationService;
import com.jalaldeveloper.accountingsystem.platform.application.dto.PageResponse;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/hr/attendances", produces = "application/json")
public class AttendanceController {

    private final AttendanceApplicationService service;

    public AttendanceController(AttendanceApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("hr.attendance.write")
    public ResponseEntity<AttendanceResponse> create(@Valid @RequestBody CreateAttendanceCommand cmd) {
        return ResponseEntity.ok(service.create(cmd));
    }

    @GetMapping("/{id}")
    @RequiresPermission("hr.attendance.read")
    public ResponseEntity<AttendanceResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequiresPermission("hr.attendance.write")
    public ResponseEntity<AttendanceResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateAttendanceCommand cmd) {
        return ResponseEntity.ok(service.update(id, cmd));
    }

    @GetMapping
    @RequiresPermission("hr.attendance.read")
    public ResponseEntity<PageResponse<AttendanceSummaryResponse>> list(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size) {
        var result = service.search(companyId, employeeId, from, to, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result, Function.identity()));
    }

    @PostMapping("/bulk")
    @RequiresPermission("hr.attendance.write")
    public ResponseEntity<List<AttendanceResponse>> bulkCreate(@Valid @RequestBody BulkCreateAttendanceCommand cmd) {
        return ResponseEntity.ok(service.bulkCreate(cmd));
    }

    @PostMapping("/generate-from-schedule")
    @RequiresPermission("hr.attendance.write")
    public ResponseEntity<List<AttendanceResponse>> generateFromSchedule(
            @Valid @RequestBody GenerateAttendanceFromScheduleCommand cmd) {
        return ResponseEntity.ok(service.generateFromSchedule(cmd));
    }

    @GetMapping("/gantt")
    @RequiresPermission("hr.attendance.read")
    public ResponseEntity<List<AttendanceSummaryResponse>> gantt(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(service.gantt(companyId, employeeId, from, to));
    }
}
