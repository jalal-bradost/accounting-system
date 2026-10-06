package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.PartnerToolSupport;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.bradox.erp.contacts.service.domain.dto.PartnerResponse;
import com.bradox.erp.contacts.service.domain.dto.PayableStatusResponse;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetVendorBalanceTool implements ErpTool {

    private final PartnerApplicationService partnerApplicationService;

    public GetVendorBalanceTool(PartnerApplicationService partnerApplicationService) {
        this.partnerApplicationService = partnerApplicationService;
    }

    @Override
    public String name() {
        return "getVendorBalance";
    }

    @Override
    public String description() {
        return "Accounts payable for one vendor partner: outstanding payable balance. "
                + "Requires partnerId (UUID) or partnerName (search vendors). "
                + "Use for 'how much do we owe vendor X', supplier balance, AP. "
                + "Not the same as purchase spend for a period.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("partnerId", Map.of("type", "string", "description", "Vendor partner UUID"));
        properties.put("partnerName", Map.of("type", "string", "description", "Vendor name to search if partnerId unknown"));
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
        PartnerToolSupport.ResolveOutcome resolved = PartnerToolSupport.resolvePartner(
                partnerApplicationService,
                context.companyId(),
                ToolSchemas.text(arguments, "partnerId"),
                ToolSchemas.text(arguments, "partnerName"),
                null,
                true,
                "vendor");
        if (resolved.needsReturn()) {
            return resolved.earlyResult();
        }

        PartnerResponse partner = partnerApplicationService.getPartner(resolved.partnerId());
        PayableStatusResponse payable = partnerApplicationService.payableStatus(resolved.partnerId());
        String currency = payable.companyCurrencyCode() != null ? payable.companyCurrencyCode() : context.currencyCode();
        String displayName = partner.getDisplayName() != null ? partner.getDisplayName() : "";
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("partnerId", resolved.partnerId().toString());
        data.put("partnerName", displayName);
        data.put("outstandingPayable", payable.outstandingPayable());
        data.put("currency", currency);
        return ToolResult.ok(data, List.of(
                ToolResult.kpiArtifact(
                        "Owed to " + (!displayName.isBlank() ? displayName : "vendor"),
                        payable.outstandingPayable(),
                        currency)));
    }
}
