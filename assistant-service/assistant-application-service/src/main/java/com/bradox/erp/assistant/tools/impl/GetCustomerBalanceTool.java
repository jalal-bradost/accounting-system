package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.bradox.erp.contacts.service.domain.dto.CreditStatusResponse;
import com.bradox.erp.contacts.service.domain.dto.PartnerResponse;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetCustomerBalanceTool implements ErpTool {

    private final PartnerApplicationService partnerApplicationService;

    public GetCustomerBalanceTool(PartnerApplicationService partnerApplicationService) {
        this.partnerApplicationService = partnerApplicationService;
    }

    @Override
    public String name() {
        return "getCustomerBalance";
    }

    @Override
    public String description() {
        return "Accounts receivable / credit status for one customer partner: outstanding receivable, credit limit, available credit. "
                + "Requires partnerId (UUID) or partnerName (search). "
                + "If multiple partners match the name, returns candidates for clarification. "
                + "This is receivables owed by the customer, not revenue.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("partnerId", Map.of("type", "string", "description", "Customer partner UUID"));
        properties.put("partnerName", Map.of("type", "string", "description", "Customer name to search if partnerId unknown"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    @Override
    public String requiredPermission() {
        return "contacts.partner.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        String partnerIdText = ToolSchemas.text(arguments, "partnerId");
        String partnerName = ToolSchemas.text(arguments, "partnerName");
        UUID partnerId = null;
        if (partnerIdText != null) {
            try {
                partnerId = UUID.fromString(partnerIdText);
            } catch (IllegalArgumentException ex) {
                return ToolResult.error("partnerId must be a valid UUID");
            }
        }
        if (partnerId == null) {
            if (partnerName == null) {
                return ToolResult.error("Provide partnerId or partnerName");
            }
            Page<PartnerResponse> page = partnerApplicationService.search(
                    context.companyId(), partnerName, true, null, false, PageRequest.of(0, 5));
            if (page.isEmpty()) {
                return ToolResult.error("No customer found matching '" + partnerName + "'");
            }
            if (page.getTotalElements() > 1) {
                List<Map<String, Object>> candidates = new ArrayList<>();
                for (PartnerResponse p : page.getContent()) {
                    candidates.add(Map.of(
                            "partnerId", p.getId().toString(),
                            "name", p.getDisplayName() != null ? p.getDisplayName() : ""));
                }
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("needsClarification", true);
                data.put("message", "Multiple customers matched. Ask the user which one.");
                data.put("candidates", candidates);
                return ToolResult.ok(data);
            }
            partnerId = page.getContent().get(0).getId();
        }

        PartnerResponse partner = partnerApplicationService.getPartner(partnerId);
        CreditStatusResponse credit = partnerApplicationService.creditStatus(partnerId);
        String currency = credit.companyCurrencyCode() != null ? credit.companyCurrencyCode() : context.currencyCode();
        String displayName = partner.getDisplayName() != null ? partner.getDisplayName() : "";
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("partnerId", partnerId.toString());
        data.put("partnerName", displayName);
        data.put("outstandingReceivable", credit.outstandingReceivable());
        data.put("creditLimit", credit.creditLimit());
        data.put("available", credit.available());
        data.put("unlimited", credit.unlimited());
        data.put("currency", currency);
        return ToolResult.ok(data, List.of(
                ToolResult.kpiArtifact(
                        (!displayName.isBlank() ? displayName : "Customer") + " owes",
                        credit.outstandingReceivable(),
                        currency)));
    }
}
