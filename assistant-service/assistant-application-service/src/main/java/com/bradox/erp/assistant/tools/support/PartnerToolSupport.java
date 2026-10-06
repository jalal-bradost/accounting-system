package com.bradox.erp.assistant.tools.support;

import com.bradox.erp.contacts.service.domain.dto.PartnerResponse;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.assistant.tools.ToolResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PartnerToolSupport {

    private PartnerToolSupport() {}

    public record ResolveOutcome(UUID partnerId, ToolResult earlyResult) {
        public boolean needsReturn() {
            return earlyResult != null;
        }
    }

    public static ResolveOutcome resolvePartner(
            PartnerApplicationService partners,
            CompanyId companyId,
            String partnerIdText,
            String partnerName,
            Boolean isCustomer,
            Boolean isVendor,
            String roleLabel) {
        UUID partnerId = null;
        if (partnerIdText != null) {
            try {
                partnerId = UUID.fromString(partnerIdText);
            } catch (IllegalArgumentException ex) {
                return new ResolveOutcome(null, ToolResult.error("partnerId must be a valid UUID"));
            }
        }
        if (partnerId != null) {
            return new ResolveOutcome(partnerId, null);
        }
        if (partnerName == null || partnerName.isBlank()) {
            return new ResolveOutcome(null, ToolResult.error("Provide partnerId or partnerName"));
        }
        Page<PartnerResponse> page = partners.search(
                companyId, partnerName, isCustomer, isVendor, false, PageRequest.of(0, 5));
        if (page.isEmpty()) {
            return new ResolveOutcome(null, ToolResult.error("No " + roleLabel + " found matching '" + partnerName + "'"));
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
            data.put("message", "Multiple " + roleLabel + "s matched. Ask the user which one.");
            data.put("candidates", candidates);
            return new ResolveOutcome(null, ToolResult.ok(data));
        }
        return new ResolveOutcome(page.getContent().get(0).getId(), null);
    }
}
