package com.bradox.erp.sales.service.domain.ports.output;

import java.util.List;
import java.util.UUID;

/**
 * Read-only checks that orders, deliveries, invoices and the ledger agree. Each method returns
 * the problems it finds; an empty list means the rule holds.
 */
public interface SalesConsistencyQueryPort {

    /** Ordered quantity below delivered or invoiced quantity on a confirmed order line. */
    List<Issue> linesOrderedBelowDeliveredOrInvoiced(UUID companyId, UUID salesOrderId);

    /** Order line invoiced quantity that differs from its posted invoices minus posted credit notes. */
    List<Issue> linesInvoicedQtyMismatch(UUID companyId, UUID salesOrderId);

    /** The same order line on more than one open (not done, not cancelled) delivery. */
    List<Issue> linesOnSeveralOpenDeliveries(UUID companyId, UUID salesOrderId);

    /** A posted invoice or credit note whose journal entry was reversed outside the document. */
    List<Issue> postedDocumentsWithReversedEntry(UUID companyId, UUID salesOrderId);

    /** A cancelled order that still has delivered or invoiced quantity. */
    List<Issue> cancelledOrdersWithActivity(UUID companyId, UUID salesOrderId);

    /** Total debits differ from total credits across posted journal entries. */
    List<Issue> unbalancedLedger(UUID companyId);

    record Issue(String check, UUID salesOrderId, String orderName, String detail) {}
}
