package com.bradox.erp.sales.service.domain.ports.input;

import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.bradox.erp.inventory.service.domain.dto.StockPickingResponse;
import com.bradox.erp.inventory.service.domain.dto.ValidatePickingCommand;
import com.bradox.erp.sales.service.domain.dto.CreateCustomerInvoiceFromSalesOrderCommand;
import com.bradox.erp.sales.service.domain.dto.CreateSalesOrderCommand;
import com.bradox.erp.sales.service.domain.dto.CreateSalesReturnCommand;
import com.bradox.erp.sales.service.domain.dto.SalesOrderResponse;
import com.bradox.erp.sales.service.domain.dto.SalesOrderSummaryResponse;
import com.bradox.erp.sales.domain.core.SalesOrderState;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SalesApplicationService {

    SalesOrderResponse createSalesOrder(@Valid CreateSalesOrderCommand command);

    SalesOrderResponse updateSalesOrder(UUID id, @Valid CreateSalesOrderCommand command);

    Page<SalesOrderSummaryResponse> searchSalesOrders(UUID companyId,
                                                           SalesOrderState state,
                                                           UUID customerPartnerId,
                                                           String q,
                                                           Pageable pageable);

    /** As above, limited to orders whose order date is within [orderDateFrom, orderDateTo] (either may be null). */
    Page<SalesOrderSummaryResponse> searchSalesOrders(UUID companyId,
                                                      SalesOrderState state,
                                                      UUID customerPartnerId,
                                                      String q,
                                                      java.time.LocalDate orderDateFrom,
                                                      java.time.LocalDate orderDateTo,
                                                      Pageable pageable);

    SalesOrderResponse getSalesOrder(UUID id);

    SalesOrderResponse sendQuotation(UUID id);

    SalesOrderResponse confirmSalesOrder(UUID id);

    SalesOrderResponse cancelSalesOrder(UUID id);

    /**
     * Guided return in one transaction: return pickings across the order's deliveries, validated
     * at the original cost; refund credits and posts, replace re-delivers, damaged goods are scrapped.
     */
    SalesOrderResponse returnGoods(UUID id, com.bradox.erp.sales.service.domain.dto.ReturnGoodsCommand command);

    /** Price/discount/tax/order-discount change after invoicing: credit at old terms, re-invoice at new. */
    com.bradox.erp.sales.service.domain.dto.SalesCorrectionResult changeTerms(
            UUID id, com.bradox.erp.sales.service.domain.dto.SalesCorrectionCommand command);

    /** Lower quantities (not below delivered); invoiced quantity above the new quantity is credited. */
    com.bradox.erp.sales.service.domain.dto.SalesCorrectionResult reduceQuantities(
            UUID id, com.bradox.erp.sales.service.domain.dto.SalesCorrectionCommand command);

    /** Return everything delivered, credit everything invoiced, then cancel the order. */
    com.bradox.erp.sales.service.domain.dto.SalesCorrectionResult cancelWithDocuments(
            UUID id, com.bradox.erp.sales.service.domain.dto.SalesCorrectionCommand command);

    /** Wrong customer before delivery: credit their invoices, move the order, re-invoice the new customer. */
    com.bradox.erp.sales.service.domain.dto.SalesCorrectionResult reassignCustomer(
            UUID id, com.bradox.erp.sales.service.domain.dto.SalesCorrectionCommand command);

    /** Customer won't take the rest: ordered = delivered on every goods line, open deliveries cancelled. */
    SalesOrderResponse closeRemainingQuantities(UUID id, String reason);

    SalesOrderResponse lockSalesOrder(UUID id);

    SalesOrderResponse unlockSalesOrder(UUID id);

    StockPickingResponse validateDeliveryPicking(UUID pickingId, ValidatePickingCommand command);

    /**
     * Creates a draft return picking from the latest DONE delivery for the sales order.
     */
    StockPickingResponse createReturnFromSalesOrder(UUID salesOrderId);

    StockPickingResponse createReturnFromSalesOrder(UUID salesOrderId, CreateSalesReturnCommand command);

    /**
     * Recomputes each line's {@code qty_delivered} from done stock moves (same rules as after delivery validate).
     */
    void syncSalesOrderLineQtyDeliveredFromStockMoves(UUID salesOrderId);

    void syncSalesOrderLineQtyDeliveredFromStockMoves(UUID salesOrderId, UUID pickingId);

    /** Updates qty_delivered in the current transaction (use from POS checkout before invoicing). */
    void refreshSalesOrderQtyDeliveredInCurrentTransaction(UUID salesOrderId);

    void afterOutgoingPickingValidated(UUID salesOrderId, UUID pickingId);

    CustomerInvoiceResponse createCustomerInvoiceFromSalesOrder(@Valid CreateCustomerInvoiceFromSalesOrderCommand command);

    CustomerInvoiceResponse createCustomerCreditNoteFromSalesOrder(@Valid CreateCustomerInvoiceFromSalesOrderCommand command);
}
