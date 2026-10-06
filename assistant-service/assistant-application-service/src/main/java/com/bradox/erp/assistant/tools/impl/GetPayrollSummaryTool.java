package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.dates.RelativeDateResolver;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.bradox.erp.hr.service.domain.dto.payroll.PayrollApi;
import com.bradox.erp.hr.service.domain.ports.input.PayRunApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetPayrollSummaryTool implements ErpTool {

    private final PayRunApplicationService payRunService;
    private final RelativeDateResolver dateResolver;

    public GetPayrollSummaryTool(PayRunApplicationService payRunService, RelativeDateResolver dateResolver) {
        this.payRunService = payRunService;
        this.dateResolver = dateResolver;
    }

    @Override
    public String name() {
        return "getPayrollSummary";
    }

    @Override
    public String description() {
        return "Payroll pay runs overlapping a period: name, period, state, payslip count, total net pay. "
                + "Use for payroll / salary / payslip totals questions.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return ToolSchemas.periodOrRangeSchema(false);
    }

    @Override
    public String requiredPermission() {
        return "payroll.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        RelativeDateResolver.DateRange range = ToolSchemas.resolveDates(dateResolver, arguments);
        LocalDate from = range.from();
        LocalDate to = range.to();
        String currency = context.currencyCode();

        List<PayrollApi.PayRunSummaryResponse> runs = payRunService.listRuns(context.companyId());
        List<Map<String, Object>> matched = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        BigDecimal totalNet = BigDecimal.ZERO;
        int totalPayslips = 0;

        for (PayrollApi.PayRunSummaryResponse run : runs) {
            if (!overlaps(run.periodStart(), run.periodEnd(), from, to)) {
                continue;
            }
            BigDecimal net = run.totalNet() != null ? run.totalNet() : BigDecimal.ZERO;
            totalNet = totalNet.add(net);
            totalPayslips += run.payslipCount();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", run.id().toString());
            row.put("name", run.name() != null ? run.name() : "");
            row.put("periodStart", run.periodStart() != null ? run.periodStart().toString() : "");
            row.put("periodEnd", run.periodEnd() != null ? run.periodEnd().toString() : "");
            row.put("state", run.state() != null ? run.state() : "");
            row.put("payslipCount", run.payslipCount());
            row.put("totalNet", net);
            matched.add(row);
            table.add(List.of(
                    run.name() != null ? run.name() : "",
                    run.periodStart() != null ? run.periodStart().toString() : "",
                    run.periodEnd() != null ? run.periodEnd().toString() : "",
                    run.state() != null ? run.state() : "",
                    run.payslipCount(),
                    net));
        }

        Map<String, Object> data = ToolSchemas.baseFinancial(range, currency);
        data.put("payRunCount", matched.size());
        data.put("payslipCount", totalPayslips);
        data.put("totalNet", totalNet);
        data.put("payRuns", matched);

        List<Map<String, Object>> artifacts = new ArrayList<>();
        artifacts.add(ToolResult.kpiArtifact("Total net pay", totalNet, currency));
        artifacts.add(ToolResult.kpiArtifact("Pay runs", matched.size(), null));
        artifacts.add(ToolResult.kpiArtifact("Payslips", totalPayslips, null));
        artifacts.add(ToolResult.tableArtifact(
                "Pay runs",
                List.of("Name", "From", "To", "State", "Payslips", "Net"),
                table,
                (long) matched.size()));
        return ToolResult.ok(data, artifacts);
    }

    private static boolean overlaps(LocalDate runStart, LocalDate runEnd, LocalDate from, LocalDate to) {
        if (runStart == null || runEnd == null) {
            return false;
        }
        return !runEnd.isBefore(from) && !runStart.isAfter(to);
    }
}
