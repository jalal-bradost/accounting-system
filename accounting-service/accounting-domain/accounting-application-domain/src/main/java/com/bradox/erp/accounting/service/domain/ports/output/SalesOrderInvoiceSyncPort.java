package com.bradox.erp.accounting.service.domain.ports.output;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Called after a customer invoice is posted when it originated from a sales order. */
public interface SalesOrderInvoiceSyncPort {

    void applyPostedInvoiceQuantities(UUID salesOrderId, Map<UUID, BigDecimal> invoicedQtyBySalesLineId);

    /**
     * Called before an order-linked invoice or credit note is posted. Refuses a document that no
     * longer fits the order: invoicing beyond the ordered quantity, crediting beyond the invoiced
     * quantity, or an invoice price that differs from the order line.
     */
    default void assertDocumentFitsOrder(UUID salesOrderId, boolean creditNote, List<DocumentLine> lines) {
    }

    record DocumentLine(UUID salesOrderLineId, BigDecimal qty, BigDecimal unitPrice, boolean gift) {}
}
