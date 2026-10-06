package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.dates.RelativeDateResolver;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.bradox.erp.sales.service.domain.SalesDashboardService;
import com.bradox.erp.sales.service.domain.dto.SalesDashboardResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetTopProductsTool implements ErpTool {

    private final SalesDashboardService salesDashboardService;
    private final RelativeDateResolver dateResolver;

    public GetTopProductsTool(SalesDashboardService salesDashboardService, RelativeDateResolver dateResolver) {
        this.salesDashboardService = salesDashboardService;
        this.dateResolver = dateResolver;
    }

    @Override
    public String name() {
        return "getTopProducts";
    }

    @Override
    public String description() {
        return "Top-selling products by sales revenue for a period "
                + "(confirmed order lines net of customer returns). "
                + "Use for 'best products', 'top 10 products', product sales ranking.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return ToolSchemas.periodOrRangeSchema(true);
    }

    @Override
    public String requiredPermission() {
        return "sales.order.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        RelativeDateResolver.DateRange range = ToolSchemas.resolveDates(dateResolver, arguments);
        int limit = ToolSchemas.limit(arguments, 10, 25);
        SalesDashboardResponse dash = salesDashboardService.getDashboard(
                context.companyId().getId(), range.from(), range.to());
        String currency = context.currencyCode();
        List<SalesDashboardResponse.SalesRankedProductRow> products = dash.getTopProducts() != null
                ? dash.getTopProducts()
                : List.of();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> tableRows = new ArrayList<>();
        int i = 0;
        for (SalesDashboardResponse.SalesRankedProductRow row : products) {
            if (i >= limit) {
                break;
            }
            Map<String, Object> rowMap = new LinkedHashMap<>();
            rowMap.put("rank", i + 1);
            rowMap.put("productId", row.getProductId() != null ? row.getProductId().toString() : "");
            rowMap.put("productName", row.getProductName() != null ? row.getProductName() : "");
            rowMap.put("orderCount", row.getOrderCount());
            rowMap.put("revenue", row.getRevenue());
            rows.add(rowMap);
            tableRows.add(List.of(
                    i + 1,
                    row.getProductName() != null ? row.getProductName() : "",
                    row.getOrderCount(),
                    row.getRevenue()));
            i++;
        }
        Map<String, Object> data = ToolSchemas.baseFinancial(range, currency);
        data.put("limit", limit);
        data.put("products", rows);
        return ToolResult.ok(data, List.of(
                ToolResult.tableArtifact("Top products", List.of("Rank", "Product", "Orders", "Revenue"), tableRows)));
    }
}
