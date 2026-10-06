package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.EntityAccess;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.bradox.erp.inventory.domain.core.valueobject.PickingState;
import com.bradox.erp.inventory.domain.core.valueobject.PickingType;
import com.bradox.erp.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.bradox.erp.purchase.domain.core.PurchaseOrderState;
import com.bradox.erp.purchase.service.domain.ports.input.PurchaseApplicationService;
import com.bradox.erp.sales.domain.core.SalesOrderState;
import com.bradox.erp.sales.service.domain.ports.input.SalesApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class GetPipelineStatusTool implements ErpTool {

    public static final Set<String> ANY_OF = Set.of(
            "sales.order.read",
            "purchase.order.read",
            "inventory.picking.read"
    );

    private final EntityAccess entityAccess;
    private final SalesApplicationService salesService;
    private final PurchaseApplicationService purchaseService;
    private final StockPickingApplicationService pickingService;

    public GetPipelineStatusTool(EntityAccess entityAccess,
                                 SalesApplicationService salesService,
                                 PurchaseApplicationService purchaseService,
                                 StockPickingApplicationService pickingService) {
        this.entityAccess = entityAccess;
        this.salesService = salesService;
        this.purchaseService = purchaseService;
        this.pickingService = pickingService;
    }

    @Override
    public String name() {
        return "getPipelineStatus";
    }

    @Override
    public String description() {
        return "Counts of open pipeline documents by state. kind: sales (orders/quotations), purchase (POs), "
                + "pickings (receipts/deliveries/internal), or all. "
                + "Use for 'how many deliveries pending', 'draft quotations', 'open purchase orders'.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("kind", Map.of(
                "type", "string",
                "description", "sales | purchase | pickings | all (default all)"));
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
    public Set<String> requiredPermissions() {
        return ANY_OF;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        String kind = ToolSchemas.text(arguments, "kind");
        if (kind == null) {
            kind = "all";
        }
        kind = kind.trim().toLowerCase(Locale.ROOT);

        Map<String, Object> data = new LinkedHashMap<>();
        List<List<Object>> table = new ArrayList<>();

        if (("sales".equals(kind) || "all".equals(kind))
                && entityAccess.can(context.userId(), "sales.order.read")) {
            Map<String, Long> byState = new LinkedHashMap<>();
            for (SalesOrderState state : SalesOrderState.values()) {
                long count = salesService.searchSalesOrders(
                                context.companyId().getId(), state, null, null, PageRequest.of(0, 1))
                        .getTotalElements();
                byState.put(state.name(), count);
                table.add(List.of("Sales order", state.name(), count));
            }
            data.put("salesOrders", byState);
        } else if ("sales".equals(kind) || "all".equals(kind)) {
            data.put("salesOrders", EntityAccess.RESTRICTED);
        }

        if (("purchase".equals(kind) || "all".equals(kind))
                && entityAccess.can(context.userId(), "purchase.order.read")) {
            Map<String, Long> byState = new LinkedHashMap<>();
            for (PurchaseOrderState state : PurchaseOrderState.values()) {
                long count = purchaseService.searchPurchaseOrders(
                                context.companyId().getId(), state, null, null, PageRequest.of(0, 1))
                        .getTotalElements();
                byState.put(state.name(), count);
                table.add(List.of("Purchase order", state.name(), count));
            }
            data.put("purchaseOrders", byState);
        } else if ("purchase".equals(kind) || "all".equals(kind)) {
            data.put("purchaseOrders", EntityAccess.RESTRICTED);
        }

        if (("pickings".equals(kind) || "all".equals(kind) || "deliveries".equals(kind) || "receipts".equals(kind))
                && entityAccess.can(context.userId(), "inventory.picking.read")) {
            Map<String, Object> pickings = new LinkedHashMap<>();
            for (PickingType type : PickingType.values()) {
                if ("deliveries".equals(kind) && type != PickingType.OUTGOING) {
                    continue;
                }
                if ("receipts".equals(kind) && type != PickingType.INCOMING) {
                    continue;
                }
                Map<String, Long> byState = new LinkedHashMap<>();
                for (PickingState state : PickingState.values()) {
                    long count = pickingService.searchPickings(
                                    context.companyId(), type, state, PageRequest.of(0, 1))
                            .getTotalElements();
                    byState.put(state.name(), count);
                    table.add(List.of("Picking " + type.name(), state.name(), count));
                }
                pickings.put(type.name(), byState);
            }
            data.put("pickings", pickings);
        } else if ("pickings".equals(kind) || "all".equals(kind) || "deliveries".equals(kind) || "receipts".equals(kind)) {
            data.put("pickings", EntityAccess.RESTRICTED);
        }

        if (data.isEmpty()) {
            return ToolResult.error("No pipeline sections available for kind='" + kind
                    + "' or missing permissions");
        }

        data.put("kind", kind);
        data.put("note", "Pending deliveries ≈ OUTGOING pickings in DRAFT/CONFIRMED/ASSIGNED. "
                + "Quotations ≈ sales orders in DRAFT or QUOTATION_SENT.");
        return ToolResult.ok(data, List.of(
                ToolResult.tableArtifact("Pipeline by state", List.of("Document", "State", "Count"), table)));
    }
}
