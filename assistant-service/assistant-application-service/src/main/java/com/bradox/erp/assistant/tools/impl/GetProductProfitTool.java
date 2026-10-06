package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.dates.RelativeDateResolver;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.bradox.erp.sales.service.domain.SalesProductProfitService;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetProductProfitTool implements ErpTool {

    private final SalesProductProfitService productProfitService;
    private final RelativeDateResolver dateResolver;

    public GetProductProfitTool(SalesProductProfitService productProfitService, RelativeDateResolver dateResolver) {
        this.productProfitService = productProfitService;
        this.dateResolver = dateResolver;
    }

    @Override
    public String name() {
        return "getProductProfit";
    }

    @Override
    public String description() {
        return "Product margin/profit for a period: estimated (ordered qty net of returns × current valuation cost) "
                + "and realized (net delivered stock moves × move cost). "
                + "Use for questions about product profit, margin %, contribution, which products make money. "
                + "mode=realized (default) or estimated. Not the same as accounting P&L net income.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("period", Map.of(
                "type", "string",
                "description", "Relative period (default THIS_MONTH if omitted): TODAY, YESTERDAY, THIS_WEEK, LAST_WEEK, THIS_MONTH, LAST_MONTH, THIS_QUARTER, LAST_QUARTER, THIS_YEAR, LAST_YEAR, LAST_30_DAYS, LAST_90_DAYS"));
        properties.put("from", Map.of("type", "string", "description", "ISO date YYYY-MM-DD (optional if period set)"));
        properties.put("to", Map.of("type", "string", "description", "ISO date YYYY-MM-DD (optional if period set)"));
        properties.put("limit", Map.of("type", "integer", "description", "Max products to return (default 10, max 25)"));
        properties.put("mode", Map.of(
                "type", "string",
                "description", "realized (default, delivered) or estimated (ordered)"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    @Override
    public String requiredPermission() {
        return "sales.order.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        RelativeDateResolver.DateRange range = ToolSchemas.resolveDates(dateResolver, arguments);
        int limit = ToolSchemas.limit(arguments, 10, 25);
        String modeRaw = ToolSchemas.text(arguments, "mode");
        boolean realized = modeRaw == null || !"estimated".equalsIgnoreCase(modeRaw.trim());

        SalesProductProfitResponse report = productProfitService.getProductProfit(
                context.companyId().getId(), range.from(), range.to());
        String currency = context.currencyCode();

        SalesProductProfitResponse.ProfitSlice totalsSlice = realized
                ? report.getTotals().getRealized()
                : report.getTotals().getEstimated();

        List<Map<String, Object>> products = new ArrayList<>();
        List<List<Object>> tableRows = new ArrayList<>();
        int i = 0;
        for (SalesProductProfitResponse.ProductProfitRow row : report.getProducts()) {
            if (i >= limit) {
                break;
            }
            SalesProductProfitResponse.ProfitSlice slice = realized ? row.getRealized() : row.getEstimated();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("rank", i + 1);
            map.put("productId", row.getProductId() != null ? row.getProductId().toString() : "");
            map.put("productName", row.getProductName() != null ? row.getProductName() : "");
            map.put("orderCount", row.getOrderCount());
            map.put("qty", realized ? row.getQtyDelivered() : row.getQtyOrdered());
            map.put("revenue", slice.getRevenue());
            map.put("cost", slice.getCost());
            map.put("profit", slice.getProfit());
            map.put("marginPercent", slice.getMarginPercent());
            products.add(map);
            tableRows.add(List.of(
                    i + 1,
                    row.getProductName() != null ? row.getProductName() : "",
                    slice.getRevenue(),
                    slice.getCost(),
                    slice.getProfit(),
                    slice.getMarginPercent()));
            i++;
        }

        Map<String, Object> data = ToolSchemas.baseFinancial(range, currency);
        data.put("mode", realized ? "realized" : "estimated");
        data.put("modeNote", realized
                ? "Realized uses delivered quantities and valuation cost."
                : "Estimated uses ordered quantities and valuation cost.");
        data.put("totalRevenue", totalsSlice.getRevenue());
        data.put("totalCost", totalsSlice.getCost());
        data.put("totalProfit", totalsSlice.getProfit());
        data.put("totalMarginPercent", totalsSlice.getMarginPercent());
        data.put("products", products);
        data.put("limit", limit);

        String modeLabel = realized ? "Realized" : "Estimated";
        return ToolResult.ok(data, List.of(
                ToolResult.kpiArtifact(modeLabel + " product profit", totalsSlice.getProfit(), currency),
                ToolResult.kpiArtifact(modeLabel + " margin %", totalsSlice.getMarginPercent(), null),
                ToolResult.tableArtifact(
                        modeLabel + " product profit",
                        List.of("Rank", "Product", "Revenue", "Cost", "Profit", "Margin %"),
                        tableRows)));
    }
}
