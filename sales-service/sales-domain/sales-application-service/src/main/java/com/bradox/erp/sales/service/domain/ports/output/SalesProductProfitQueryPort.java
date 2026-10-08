package com.bradox.erp.sales.service.domain.ports.output;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SalesProductProfitQueryPort {

    /** Confirmed non-gift product sales in the order/confirm date range (qty/revenue net of returns through {@code to}). */
    record ConfirmedProductFact(
            UUID productId,
            String productName,
            long orderCount,
            BigDecimal qtyOrdered,
            BigDecimal revenue) {}

    /**
     * Invoiced sales per product in the invoice date range, exactly as posted to the ledger: lines
     * after line and order discounts (gift lines at full price, as they credit revenue), credit notes
     * deducted, invoices whose journal entry was reversed left out. {@code productId} is null for
     * invoice lines that do not come from a sales order line.
     */
    record InvoicedProductFact(
            UUID productId,
            String productName,
            BigDecimal qtyInvoiced,
            BigDecimal revenue) {}

    /**
     * Cost of goods sold per key (product), as the COGS clearing posts it: cleared
     * qty × average delivered unit cost of the order line, dated at the order date.
     */
    record CostFact(UUID key, BigDecimal cost) {}

    /**
     * One sales order line: stockable units invoiced in the period whose cost of goods sold is not
     * booked yet because they are not delivered. {@code revenue} is what those units contributed to
     * invoiced net sales; {@code firstInvoiceDate} is when they were first invoiced in the period.
     */
    record PendingDeliveryLine(
            UUID orderId,
            String orderName,
            String customerName,
            UUID productId,
            BigDecimal units,
            BigDecimal revenue,
            LocalDate firstInvoiceDate) {}

    /** Orders and their value at one stage of the sales pipeline (company currency, before tax). */
    record PipelineFact(long orders, BigDecimal amount) {}

    /** Ledger totals for the period, as on the Profit &amp; Loss. */
    record LedgerProfitTotals(BigDecimal netSales, BigDecimal costOfRevenue) {}

    List<ConfirmedProductFact> confirmedProductFacts(UUID companyId, LocalDate from, LocalDate to);

    List<InvoicedProductFact> invoicedProductFacts(UUID companyId, LocalDate from, LocalDate to);

    List<PendingDeliveryLine> pendingDeliveryLines(UUID companyId, LocalDate from, LocalDate to);

    /** Quotations (draft or sent, not confirmed) dated in the period: no financial impact yet. */
    PipelineFact quotationPipeline(UUID companyId, LocalDate from, LocalDate to);

    /** Confirmed orders in the period: the part of their lines not invoiced yet. */
    PipelineFact confirmedNotInvoiced(UUID companyId, LocalDate from, LocalDate to);

    List<CostFact> costOfSalesByProduct(UUID companyId, LocalDate from, LocalDate to);

    LedgerProfitTotals ledgerProfitTotals(UUID companyId, LocalDate from, LocalDate to);

    /**
     * Current valuation unit cost per product: {@code SVL value / on-hand qty} when on-hand &gt; 0,
     * otherwise {@code standard_cost}. Non-stockable products map to zero.
     */
    Map<UUID, BigDecimal> currentValuationUnitCosts(UUID companyId, Collection<UUID> productIds);
}
