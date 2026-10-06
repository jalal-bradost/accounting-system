package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.accounting.service.domain.ports.input.service.ReportingApplicationService;
import com.bradox.erp.accounting.service.domain.report.ProfitAndLossReport;
import com.bradox.erp.assistant.dates.RelativeDateResolver;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GetProfitAndLossTool implements ErpTool {

    private final ReportingApplicationService reportingApplicationService;
    private final RelativeDateResolver dateResolver;

    public GetProfitAndLossTool(ReportingApplicationService reportingApplicationService,
                                RelativeDateResolver dateResolver) {
        this.reportingApplicationService = reportingApplicationService;
        this.dateResolver = dateResolver;
    }

    @Override
    public String name() {
        return "getProfitAndLoss";
    }

    @Override
    public String description() {
        return "Authoritative accounting profit and loss (income statement) for a period. "
                + "Returns totalRevenue, totalExpenses, and netIncome from the ERP ledger. "
                + "Use for questions about net profit, P&L, revenue from accounting (not sales-order revenue), or expenses on the income statement. "
                + "netIncome ≠ cash collected. Accounting revenue may differ from sales dashboard revenue.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return ToolSchemas.periodOrRangeSchema(false);
    }

    @Override
    public String requiredPermission() {
        return "accounting.report.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        RelativeDateResolver.DateRange range = ToolSchemas.resolveDates(dateResolver, arguments);
        ProfitAndLossReport report = reportingApplicationService.getProfitAndLoss(
                context.companyId().getId(), range.from(), range.to());
        String currency = context.currencyCode();
        Map<String, Object> data = ToolSchemas.baseFinancial(range, currency);
        data.put("totalRevenue", report.totalRevenue());
        data.put("totalExpenses", report.totalExpenses());
        data.put("netIncome", report.netIncome());
        List<Map<String, Object>> artifacts = new ArrayList<>();
        artifacts.add(ToolResult.kpiArtifact("Total revenue", report.totalRevenue(), currency));
        artifacts.add(ToolResult.kpiArtifact("Total expenses", report.totalExpenses(), currency));
        artifacts.add(ToolResult.kpiArtifact("Net income", report.netIncome(), currency));
        return ToolResult.ok(data, artifacts);
    }
}
