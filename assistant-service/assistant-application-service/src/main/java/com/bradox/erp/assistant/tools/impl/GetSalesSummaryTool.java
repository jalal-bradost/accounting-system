package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.dates.RelativeDateResolver;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.bradox.erp.sales.service.domain.SalesDashboardService;
import com.bradox.erp.sales.service.domain.dto.SalesDashboardResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GetSalesSummaryTool implements ErpTool {

    private final SalesDashboardService salesDashboardService;
    private final RelativeDateResolver dateResolver;

    public GetSalesSummaryTool(SalesDashboardService salesDashboardService, RelativeDateResolver dateResolver) {
        this.salesDashboardService = salesDashboardService;
        this.dateResolver = dateResolver;
    }

    @Override
    public String name() {
        return "getSalesSummary";
    }

    @Override
    public String description() {
        return "Sales operations summary for a period from confirmed sales orders (dashboard KPIs): "
                + "sales revenue (ordered qty net of customer returns), order count (orders with remaining net qty), "
                + "quotation count, average order value. "
                + "This is sales-order revenue, NOT the accounting P&L revenue. "
                + "Use for 'how much did we sell' / sales volume questions.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return ToolSchemas.periodOrRangeSchema(false);
    }

    @Override
    public String requiredPermission() {
        return "sales.order.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        RelativeDateResolver.DateRange range = ToolSchemas.resolveDates(dateResolver, arguments);
        SalesDashboardResponse dash = salesDashboardService.getDashboard(
                context.companyId().getId(), range.from(), range.to());
        String currency = context.currencyCode();
        BigDecimal revenue = kpiValue(dash.getRevenue());
        BigDecimal orders = kpiValue(dash.getOrders());
        BigDecimal quotations = kpiValue(dash.getQuotations());
        BigDecimal averageOrder = kpiValue(dash.getAverageOrder());
        Map<String, Object> data = ToolSchemas.baseFinancial(range, currency);
        data.put("salesRevenue", revenue);
        data.put("orderCount", orders);
        data.put("quotationCount", quotations);
        data.put("averageOrderValue", averageOrder);
        data.put("note", "salesRevenue is confirmed order totals net of customer returns; not accounting P&L revenue");
        List<Map<String, Object>> artifacts = new ArrayList<>();
        artifacts.add(ToolResult.kpiArtifact("Sales revenue", revenue, currency));
        artifacts.add(ToolResult.kpiArtifact("Orders", orders, null));
        return ToolResult.ok(data, artifacts);
    }

    private static BigDecimal kpiValue(SalesDashboardResponse.SalesKpiMetric metric) {
        return metric != null && metric.getValue() != null ? metric.getValue() : BigDecimal.ZERO;
    }
}
