package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.bradox.erp.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.bradox.erp.assistant.dates.RelativeDateResolver;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetCashCollectedTool implements ErpTool {

    private final CustomerInvoiceApplicationService invoiceService;
    private final RelativeDateResolver dateResolver;

    public GetCashCollectedTool(CustomerInvoiceApplicationService invoiceService,
                                RelativeDateResolver dateResolver) {
        this.invoiceService = invoiceService;
        this.dateResolver = dateResolver;
    }

    @Override
    public String name() {
        return "getCashCollected";
    }

    @Override
    public String description() {
        return "Customer cash collected (posted payments minus refunds) for a period from customer payment register. "
                + "Use for 'how much cash did we collect', money received, receipts this month. "
                + "Not the same as sales revenue or P&L revenue.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return ToolSchemas.periodOrRangeSchema(false);
    }

    @Override
    public String requiredPermission() {
        return "accounting.customer-invoice.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        RelativeDateResolver.DateRange range = ToolSchemas.resolveDates(dateResolver, arguments);
        List<CustomerPaymentResponse> payments = invoiceService.listCustomerPayments(context.companyId().getId());

        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        int paymentCount = 0;
        int refundCount = 0;
        List<Map<String, Object>> sample = new ArrayList<>();

        for (CustomerPaymentResponse p : payments) {
            if (p == null || !"POSTED".equalsIgnoreCase(p.getState())) {
                continue;
            }
            if (p.getPaymentDate() == null) {
                continue;
            }
            LocalDate day = p.getPaymentDate().toLocalDate();
            if (day.isBefore(range.from()) || day.isAfter(range.to())) {
                continue;
            }
            BigDecimal amount = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
            BigDecimal companyAmount = amount;
            if (p.getExchangeRateToCompany() != null && p.getExchangeRateToCompany().signum() > 0) {
                companyAmount = amount.multiply(p.getExchangeRateToCompany()).setScale(4, RoundingMode.HALF_UP);
            }
            boolean refund = "REFUND".equalsIgnoreCase(p.getPaymentKind());
            if (refund) {
                refunds = refunds.add(companyAmount);
                refundCount++;
            } else {
                collected = collected.add(companyAmount);
                paymentCount++;
            }
            if (sample.size() < 10) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("paymentId", p.getId() != null ? p.getId().toString() : "");
                row.put("date", day.toString());
                row.put("kind", refund ? "REFUND" : "PAYMENT");
                row.put("amountCompany", companyAmount);
                row.put("currency", p.getCurrencyCode());
                row.put("reference", p.getReference() != null ? p.getReference() : "");
                sample.add(row);
            }
        }

        BigDecimal net = collected.subtract(refunds).setScale(4, RoundingMode.HALF_UP);
        String currency = context.currencyCode();
        Map<String, Object> data = ToolSchemas.baseFinancial(range, currency);
        data.put("paymentsTotal", collected);
        data.put("refundsTotal", refunds);
        data.put("netCashCollected", net);
        data.put("paymentCount", paymentCount);
        data.put("refundCount", refundCount);
        data.put("sample", sample);
        data.put("note", "Amounts converted to company currency when exchange rate is present.");

        return ToolResult.ok(data, List.of(
                ToolResult.kpiArtifact("Net cash collected", net, currency),
                ToolResult.kpiArtifact("Payments", collected, currency),
                ToolResult.kpiArtifact("Refunds", refunds, currency)));
    }
}
