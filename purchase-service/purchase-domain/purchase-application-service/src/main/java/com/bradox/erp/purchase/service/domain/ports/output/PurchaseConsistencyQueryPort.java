package com.bradox.erp.purchase.service.domain.ports.output;

import java.util.List;
import java.util.UUID;

/**
 * Read-only checks that purchase orders, receipts, vendor bills and the ledger agree. Each method
 * returns the problems it finds; an empty list means the rule holds.
 */
public interface PurchaseConsistencyQueryPort {

    /** Ordered quantity below received or billed quantity on a confirmed order line. */
    List<Issue> linesOrderedBelowReceivedOrBilled(UUID companyId, UUID purchaseOrderId);

    /** Order line billed quantity that differs from its posted bills minus posted credit notes. */
    List<Issue> linesBilledQtyMismatch(UUID companyId, UUID purchaseOrderId);

    /** The same order line on more than one open (not done, not cancelled) receipt. */
    List<Issue> linesOnSeveralOpenReceipts(UUID companyId, UUID purchaseOrderId);

    /** A posted bill or credit note whose journal entry was reversed outside the document. */
    List<Issue> postedDocumentsWithReversedEntry(UUID companyId, UUID purchaseOrderId);

    /** A cancelled order that still has received or billed quantity. */
    List<Issue> cancelledOrdersWithActivity(UUID companyId, UUID purchaseOrderId);

    record Issue(String check, UUID purchaseOrderId, String orderName, String detail) {}
}
