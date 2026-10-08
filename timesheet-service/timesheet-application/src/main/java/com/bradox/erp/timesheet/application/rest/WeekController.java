package com.bradox.erp.timesheet.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.dto.BulkApproveCommand;
import com.bradox.erp.timesheet.service.domain.dto.BulkApproveResponse;
import com.bradox.erp.timesheet.service.domain.dto.SubmitWeekCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekReasonCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekSummaryResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.WeekApplicationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/timesheet/weeks", produces = "application/json")
public class WeekController {

    private final WeekApplicationService weeks;

    public WeekController(WeekApplicationService weeks) {
        this.weeks = weeks;
    }

    /** {@code scope}: APPROVE (weeks I can approve), TEAM, ALL or MINE. */
    @GetMapping
    @RequiresPermission("tsh.entry.own")
    public List<WeekSummaryResponse> list(@CurrentCompany CompanyId companyId,
                                          @RequestParam(defaultValue = "MINE") WeekApplicationService.Scope scope,
                                          @RequestParam(required = false) WeekStatus status,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return weeks.list(companyId, scope, status, from, to);
    }

    @GetMapping("/{id}")
    @RequiresPermission("tsh.entry.own")
    public WeekSummaryResponse get(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return weeks.get(companyId, id);
    }

    @PostMapping("/submit")
    @RequiresPermission("tsh.entry.own")
    public WeekSummaryResponse submit(@CurrentCompany CompanyId companyId, @Valid @RequestBody SubmitWeekCommand command) {
        return weeks.submit(companyId, command);
    }

    @PostMapping("/{id}/approve")
    @RequiresPermission("tsh.entry.own")
    public WeekSummaryResponse approve(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return weeks.approve(companyId, id);
    }

    @PostMapping("/{id}/refuse")
    @RequiresPermission("tsh.entry.own")
    public WeekSummaryResponse refuse(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                      @Valid @RequestBody WeekReasonCommand command) {
        return weeks.refuse(companyId, id, command);
    }

    @PostMapping("/{id}/reopen")
    @RequiresPermission("tsh.reopen")
    public WeekSummaryResponse reopen(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                      @Valid @RequestBody WeekReasonCommand command) {
        return weeks.reopen(companyId, id, command);
    }

    @PostMapping("/bulk-approve")
    @RequiresPermission("tsh.entry.own")
    public BulkApproveResponse bulkApprove(@CurrentCompany CompanyId companyId, @Valid @RequestBody BulkApproveCommand command) {
        return weeks.bulkApprove(companyId, command);
    }
}
