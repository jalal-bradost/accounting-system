package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.bradox.erp.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.InvoiceToolSupport;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.bradox.erp.contacts.service.domain.dto.PartnerResponse;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GetOverdueInvoicesTool implements ErpTool {

    private final CustomerInvoiceApplicationService invoiceService;
    private final PartnerApplicationService partnerApplicationService;

    public GetOverdueInvoicesTool(CustomerInvoiceApplicationService invoiceService,
                                  PartnerApplicationService partnerApplicationService) {
        this.invoiceService = invoiceService;
        this.partnerApplicationService = partnerApplicationService;
    }

    @Override
    public String name() {
        return "getOverdueInvoices";
    }

    @Override
    public String description() {
        return "Posted customer invoices that are past due with remaining outstanding balance. "
                + "Use for overdue receivables, unpaid past-due invoices, AR aging of overdue docs. "
                + "Optional partnerId/partnerName to filter one customer. "
                + "limit defaults to 15 (max 40).";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("partnerId", Map.of("type", "string", "description", "Optional customer partner UUID"));
        properties.put("partnerName", Map.of("type", "string", "description", "Optional customer name filter"));
        properties.put("limit", Map.of("type", "integer", "description", "Max overdue invoices to return (default 15, max 40)"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    @Override
    public String requiredPermission() {
        return "accounting.customer-invoice.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        UUID filterPartner = null;
        String partnerIdText = ToolSchemas.text(arguments, "partnerId");
        String partnerName = ToolSchemas.text(arguments, "partnerName");
        if (partnerIdText != null) {
            try {
                filterPartner = UUID.fromString(partnerIdText);
            } catch (IllegalArgumentException ex) {
                return ToolResult.error("partnerId must be a valid UUID");
            }
        } else if (partnerName != null) {
            var page = partnerApplicationService.search(
                    context.companyId(), partnerName, true, null, false,
                    org.springframework.data.domain.PageRequest.of(0, 5));
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
                data.put("message", "Multiple customers matched. Ask which one.");
                data.put("candidates", candidates);
                return ToolResult.ok(data);
            }
            filterPartner = page.getContent().get(0).getId();
        }

        int limit = ToolSchemas.limit(arguments, 15, 40);
        LocalDate today = LocalDate.now();
        List<CustomerInvoiceResponse> invoices = invoiceService.listCustomerInvoices(context.companyId().getId());
        List<CustomerInvoiceResponse> candidates = new ArrayList<>();
        for (CustomerInvoiceResponse inv : invoices) {
            if (!InvoiceToolSupport.isPostedInvoice(inv)) {
                continue;
            }
            if (inv.getDueDate() == null || !inv.getDueDate().isBefore(today)) {
                continue;
            }
            if (filterPartner != null && (inv.getCustomerPartnerId() == null
                    || !filterPartner.equals(inv.getCustomerPartnerId()))) {
                continue;
            }
            candidates.add(inv);
        }

        Map<UUID, BigDecimal> paid = invoiceService.sumPostedPaymentsByInvoiceIds(
                candidates.stream().map(CustomerInvoiceResponse::getId).collect(Collectors.toList()));

        // Credit notes reducing original invoices
        Map<UUID, BigDecimal> credited = new HashMap<>();
        for (CustomerInvoiceResponse inv : invoices) {
            if (inv == null || inv.getMoveType() == null || inv.getState() == null) {
                continue;
            }
            if (inv.getMoveType().name().equals("CREDIT_NOTE")
                    && inv.getState().name().equals("POSTED")
                    && inv.getReversedInvoiceId() != null) {
                credited.merge(inv.getReversedInvoiceId(), InvoiceToolSupport.documentTotal(inv), BigDecimal::add);
            }
        }

        BigDecimal totalOutstanding = BigDecimal.ZERO;
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> tableRows = new ArrayList<>();
        Map<UUID, String> partnerNames = new HashMap<>();

        candidates.sort((a, b) -> {
            LocalDate da = a.getDueDate() != null ? a.getDueDate() : LocalDate.MIN;
            LocalDate db = b.getDueDate() != null ? b.getDueDate() : LocalDate.MIN;
            return da.compareTo(db);
        });

        for (CustomerInvoiceResponse inv : candidates) {
            BigDecimal total = InvoiceToolSupport.documentTotal(inv);
            BigDecimal paidAmt = paid.getOrDefault(inv.getId(), BigDecimal.ZERO);
            BigDecimal creditAmt = credited.getOrDefault(inv.getId(), BigDecimal.ZERO);
            BigDecimal outstanding = total.subtract(paidAmt).subtract(creditAmt).setScale(4, RoundingMode.HALF_UP);
            if (outstanding.signum() <= 0) {
                continue;
            }
            totalOutstanding = totalOutstanding.add(outstanding);
            if (rows.size() >= limit) {
                continue;
            }
            String customerName = partnerNames.computeIfAbsent(inv.getCustomerPartnerId(), id -> {
                if (id == null) {
                    return "";
                }
                try {
                    PartnerResponse p = partnerApplicationService.getPartner(id);
                    return p.getDisplayName() != null ? p.getDisplayName() : "";
                } catch (Exception ex) {
                    return id.toString();
                }
            });
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(inv.getDueDate(), today);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("invoiceId", inv.getId().toString());
            row.put("reference", inv.getReference() != null ? inv.getReference() : "");
            row.put("customerPartnerId", inv.getCustomerPartnerId() != null ? inv.getCustomerPartnerId().toString() : "");
            row.put("customerName", customerName);
            row.put("invoiceDate", inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : "");
            row.put("dueDate", inv.getDueDate().toString());
            row.put("daysOverdue", daysOverdue);
            row.put("invoiceTotal", total);
            row.put("outstanding", outstanding);
            row.put("currency", inv.getCurrencyCode() != null ? inv.getCurrencyCode() : context.currencyCode());
            rows.add(row);
            tableRows.add(List.of(
                    inv.getReference() != null ? inv.getReference() : inv.getId().toString().substring(0, 8),
                    customerName,
                    inv.getDueDate().toString(),
                    daysOverdue,
                    outstanding));
        }

        String currency = context.currencyCode();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("asOf", today.toString());
        data.put("currency", currency);
        data.put("overdueCount", rows.size());
        data.put("totalOutstanding", totalOutstanding.setScale(4, RoundingMode.HALF_UP));
        data.put("invoices", rows);
        data.put("limit", limit);
        if (filterPartner != null) {
            data.put("filteredPartnerId", filterPartner.toString());
        }

        return ToolResult.ok(data, List.of(
                ToolResult.kpiArtifact("Overdue outstanding", totalOutstanding, currency),
                ToolResult.kpiArtifact("Overdue invoices shown", rows.size(), null),
                ToolResult.tableArtifact(
                        "Overdue invoices",
                        List.of("Invoice", "Customer", "Due", "Days", "Outstanding"),
                        tableRows)));
    }
}
