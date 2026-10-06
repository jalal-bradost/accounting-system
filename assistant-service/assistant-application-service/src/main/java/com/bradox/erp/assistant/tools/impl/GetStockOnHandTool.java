package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.bradox.erp.inventory.service.domain.dto.ProductResponse;
import com.bradox.erp.inventory.service.domain.dto.StockQuantResponse;
import com.bradox.erp.inventory.service.domain.ports.input.ProductApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.StockValuationApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetStockOnHandTool implements ErpTool {

    private final StockValuationApplicationService stockService;
    private final ProductApplicationService productService;

    public GetStockOnHandTool(StockValuationApplicationService stockService,
                              ProductApplicationService productService) {
        this.stockService = stockService;
        this.productService = productService;
    }

    @Override
    public String name() {
        return "getStockOnHand";
    }

    @Override
    public String description() {
        return "Inventory quantity on hand for a product (total and by location). "
                + "Provide productId or productName. "
                + "Optional belowThreshold: when scanning by name with multiple matches, only keep products at or below that qty. "
                + "Use for stock levels, on-hand quantity, 'do we have product X'.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("productId", Map.of("type", "string", "description", "Product UUID"));
        properties.put("productName", Map.of("type", "string", "description", "Product name or SKU search"));
        properties.put("belowThreshold", Map.of(
                "type", "number",
                "description", "Optional: when multiple products match, only include those with on-hand <= this qty"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    @Override
    public String requiredPermission() {
        return "inventory.product.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        String productIdText = ToolSchemas.text(arguments, "productId");
        String productName = ToolSchemas.text(arguments, "productName");
        BigDecimal below = null;
        if (arguments != null && arguments.has("belowThreshold") && !arguments.get("belowThreshold").isNull()) {
            below = BigDecimal.valueOf(arguments.get("belowThreshold").asDouble());
        }

        UUID productId = null;
        if (productIdText != null) {
            try {
                productId = UUID.fromString(productIdText);
            } catch (IllegalArgumentException ex) {
                return ToolResult.error("productId must be a valid UUID");
            }
        }

        if (productId != null) {
            return stockForProduct(context, productId, null);
        }
        if (productName == null) {
            return ToolResult.error("Provide productId or productName");
        }

        Page<ProductResponse> page = productService.searchProducts(
                context.companyId(), productName, false, PageRequest.of(0, 10));
        if (page.isEmpty()) {
            return ToolResult.error("No product found matching '" + productName + "'");
        }
        if (page.getTotalElements() == 1) {
            ProductResponse only = page.getContent().get(0);
            return stockForProduct(context, only.getId(), only.getName());
        }

        List<Map<String, Object>> matches = new ArrayList<>();
        List<List<Object>> tableRows = new ArrayList<>();
        for (ProductResponse p : page.getContent()) {
            BigDecimal onHand = stockService.totalOnHand(context.companyId(), p.getId());
            if (below != null && onHand.compareTo(below) > 0) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", p.getId().toString());
            row.put("sku", p.getSku() != null ? p.getSku() : "");
            row.put("name", p.getName() != null ? p.getName() : "");
            row.put("totalOnHand", onHand);
            matches.add(row);
            tableRows.add(List.of(
                    p.getSku() != null ? p.getSku() : "",
                    p.getName() != null ? p.getName() : "",
                    onHand));
        }
        if (matches.isEmpty()) {
            return ToolResult.error(below != null
                    ? "No matching products at or below on-hand " + below
                    : "No matching products with stock data");
        }
        if (matches.size() == 1) {
            Map<String, Object> only = matches.get(0);
            return stockForProduct(context, UUID.fromString(only.get("productId").toString()), only.get("name").toString());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("needsClarification", true);
        data.put("message", "Multiple products matched. Ask which product, or call again with productId.");
        data.put("candidates", matches);
        return ToolResult.ok(data, List.of(
                ToolResult.tableArtifact("Matching products", List.of("SKU", "Name", "On hand"), tableRows)));
    }

    private ToolResult stockForProduct(ToolContext context, UUID productId, String knownName) {
        ProductResponse product;
        try {
            product = productService.getProduct(productId);
        } catch (Exception ex) {
            return ToolResult.error("Product not found: " + productId);
        }
        BigDecimal total = stockService.totalOnHand(context.companyId(), productId);
        List<StockQuantResponse> quants = stockService.onHandByProduct(context.companyId(), productId);
        String name = knownName != null ? knownName : (product.getName() != null ? product.getName() : "");
        List<Map<String, Object>> locations = new ArrayList<>();
        List<List<Object>> tableRows = new ArrayList<>();
        for (StockQuantResponse q : quants) {
            Map<String, Object> loc = new LinkedHashMap<>();
            loc.put("locationId", q.getLocationId() != null ? q.getLocationId().toString() : "");
            loc.put("quantity", q.getQuantity());
            loc.put("reservedQuantity", q.getReservedQuantity());
            loc.put("availableQuantity", q.getAvailableQuantity());
            locations.add(loc);
            tableRows.add(List.of(
                    q.getLocationId() != null ? q.getLocationId().toString() : "",
                    q.getQuantity(),
                    q.getReservedQuantity(),
                    q.getAvailableQuantity()));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productId", productId.toString());
        data.put("sku", product.getSku() != null ? product.getSku() : "");
        data.put("productName", name);
        data.put("totalOnHand", total);
        data.put("locations", locations);
        return ToolResult.ok(data, List.of(
                ToolResult.kpiArtifact((!name.isBlank() ? name : "Product") + " on hand", total, null),
                ToolResult.tableArtifact("Stock by location", List.of("Location", "Qty", "Reserved", "Available"), tableRows)));
    }
}
