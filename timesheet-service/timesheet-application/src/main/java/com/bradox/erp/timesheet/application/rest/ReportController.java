package com.bradox.erp.timesheet.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.dto.DashboardResponse;
import com.bradox.erp.timesheet.service.domain.dto.ProfitabilityResponse;
import com.bradox.erp.timesheet.service.domain.dto.RecomputeCostResponse;
import com.bradox.erp.timesheet.service.domain.dto.ReportResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.CostApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.input.ReportApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.input.ReportApplicationService.GroupBy;
import com.bradox.erp.timesheet.service.domain.ports.input.ReportApplicationService.Query;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Reports, dashboard and cost (TSH-07, TSH-08). Scope and cost visibility are decided in the service. */
@RestController
@RequestMapping(value = "/api/v1/timesheet", produces = "application/json")
public class ReportController {

    private final ReportApplicationService reports;
    private final CostApplicationService cost;

    public ReportController(ReportApplicationService reports, CostApplicationService cost) {
        this.reports = reports;
        this.cost = cost;
    }

    @GetMapping("/reports/summary")
    @RequiresPermission("tsh.entry.own")
    public ReportResponse summary(@CurrentCompany CompanyId companyId,
                                  @RequestParam(defaultValue = "PROJECT") GroupBy groupBy,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) UUID projectId,
                                  @RequestParam(required = false) Boolean billable,
                                  @RequestParam(required = false) List<WeekStatus> status) {
        return reports.summary(companyId, new Query(groupBy, from, to, projectId, billable, status));
    }

    @GetMapping(value = "/reports/export", produces = "text/csv")
    @RequiresPermission("tsh.entry.own")
    public ResponseEntity<byte[]> export(@CurrentCompany CompanyId companyId,
                                         @RequestParam(defaultValue = "PROJECT") GroupBy groupBy,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) UUID projectId,
                                         @RequestParam(required = false) Boolean billable,
                                         @RequestParam(required = false) List<WeekStatus> status) {
        String csv = reports.exportCsv(companyId, new Query(groupBy, from, to, projectId, billable, status));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"timesheet-" + groupBy.name().toLowerCase() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/dashboard")
    @RequiresPermission("tsh.entry.own")
    public DashboardResponse dashboard(@CurrentCompany CompanyId companyId) {
        return reports.dashboard(companyId);
    }

    @GetMapping("/projects/{id}/profitability")
    @RequiresPermission("tsh.cost.view")
    public ProfitabilityResponse profitability(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return cost.profitability(companyId, id);
    }

    @PostMapping("/entries/recompute-missing-cost")
    @RequiresPermission("tsh.cost.view")
    public RecomputeCostResponse recompute(@CurrentCompany CompanyId companyId) {
        return cost.recomputeMissing(companyId);
    }
}
