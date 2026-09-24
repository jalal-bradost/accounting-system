package com.bradox.delin.sales.service.domain.ports.input;

import com.bradox.delin.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.bradox.delin.inventory.service.domain.dto.StockPickingResponse;
import com.bradox.delin.inventory.service.domain.dto.ValidatePickingCommand;
import com.bradox.delin.sales.service.domain.dto.CreateCustomerInvoiceFromSalesOrderCommand;
import com.bradox.delin.sales.service.domain.dto.CreateSalesOrderCommand;
import com.bradox.delin.sales.service.domain.dto.CreateSalesReturnCommand;
import com.bradox.delin.sales.service.domain.dto.SalesOrderResponse;
import com.bradox.delin.sales.service.domain.dto.SalesOrderSummaryResponse;
import com.bradox.delin.sales.domain.core.SalesOrderState;
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

    SalesOrderResponse getSalesOrder(UUID id);

    SalesOrderResponse sendQuotation(UUID id);

    SalesOrderResponse confirmSalesOrder(UUID id);

    SalesOrderResponse cancelSalesOrder(UUID id);

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
