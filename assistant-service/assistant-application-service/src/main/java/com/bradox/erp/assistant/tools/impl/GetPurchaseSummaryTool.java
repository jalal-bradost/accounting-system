package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.dates.RelativeDateResolver;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.bradox.erp.purchase.service.domain.PurchaseDashboardService;
import com.bradox.erp.purchase.service.domain.dto.PurchaseDashboardResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GetPurchaseSummaryTool implements ErpTool {

    private final PurchaseDashboardService purchaseDashboardService;
    private final RelativeDateResolver dateResolver;

    public GetPurchaseSummaryTool(PurchaseDashboardService purchaseDashboardService,
                                  RelativeDateResolver dateResolver) {
        this.purchaseDashboardService = purchaseDashboardService;
        this.dateResolver = dateResolver;
    }

    @Override
    public String name() {
        return "getPurchaseSummary";
    }

    @Override
    public String description() {
        return "Purchase operations summary for a period: total spend, purchase order count, RFQ count, average order. "
                + "Purchase spend is not the same as P&L expenses in every accounting context.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return ToolSchemas.periodOrRangeSchema(false);
    }

    @Override
    public String requiredPermission() {
        return "purchase.order.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        RelativeDateResolver.DateRange range = ToolSchemas.resolveDates(dateResolver, arguments);
        PurchaseDashboardResponse dash = purchaseDashboardService.getDashboard(
                context.companyId().getId(), range.from(), range.to());
        String currency = context.currencyCode();
        BigDecimal spend = kpiValue(dash.getSpend());
        BigDecimal orders = kpiValue(dash.getOrders());
        BigDecimal rfqs = kpiValue(dash.getRfqs());
        BigDecimal averageOrder = kpiValue(dash.getAverageOrder());
        Map<String, Object> data = ToolSchemas.baseFinancial(range, currency);
        data.put("spend", spend);
        data.put("orderCount", orders);
        data.put("rfqCount", rfqs);
        data.put("averageOrderValue", averageOrder);
        List<Map<String, Object>> artifacts = new ArrayList<>();
        artifacts.add(ToolResult.kpiArtifact("Purchase spend", spend, currency));
        artifacts.add(ToolResult.kpiArtifact("Purchase orders", orders, null));
        return ToolResult.ok(data, artifacts);
    }

    private static BigDecimal kpiValue(PurchaseDashboardResponse.KpiMetric metric) {
        return metric != null && metric.getValue() != null ? metric.getValue() : BigDecimal.ZERO;
    }
}
