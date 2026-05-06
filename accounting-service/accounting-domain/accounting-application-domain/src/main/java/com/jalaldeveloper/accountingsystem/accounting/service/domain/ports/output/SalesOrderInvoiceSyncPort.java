package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Called after a customer invoice is posted when it originated from a sales order. */
public interface SalesOrderInvoiceSyncPort {

    void applyPostedInvoiceQuantities(UUID salesOrderId, Map<UUID, BigDecimal> invoicedQtyBySalesLineId);
}
