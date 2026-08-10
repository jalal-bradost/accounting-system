package com.jalaldeveloper.accountingsystem.sales.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateCustomerInvoiceFromSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderSummaryResponse;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderState;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SalesApplicationService {

    SalesOrderResponse createSalesOrder(@Valid CreateSalesOrderCommand command);

    Page<SalesOrderSummaryResponse> searchSalesOrders(UUID companyId,
                                                      SalesOrderState state,
                                                      UUID customerPartnerId,
                                                      String q,
                                                      Pageable pageable);

    SalesOrderResponse getSalesOrder(UUID id);

    SalesOrderResponse sendQuotation(UUID id);

    SalesOrderResponse confirmSalesOrder(UUID id);

    SalesOrderResponse cancelSalesOrder(UUID id);

    void afterOutgoingPickingValidated(UUID salesOrderId);

    CustomerInvoiceResponse createCustomerInvoiceFromSalesOrder(@Valid CreateCustomerInvoiceFromSalesOrderCommand command);

    CustomerInvoiceResponse createCustomerCreditNoteFromSalesOrder(@Valid CreateCustomerInvoiceFromSalesOrderCommand command);
}
