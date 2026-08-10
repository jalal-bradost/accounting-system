package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CreateCreditNoteFromInvoiceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CreateCustomerInvoiceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CustomerInvoiceApplicationService {

    boolean hasPostedInvoiceForSalesOrder(UUID salesOrderId);

    /** Quantities reserved on draft customer invoices keyed by sales order line id. */
    Map<UUID, BigDecimal> draftAllocatedQtyBySalesOrderLine(UUID salesOrderId);

    /** Quantities reserved on draft customer credit notes keyed by sales order line id. */
    Map<UUID, BigDecimal> draftCreditNoteAllocatedQtyBySalesOrderLine(UUID salesOrderId);

    CustomerInvoiceResponse createCustomerInvoice(@Valid CreateCustomerInvoiceCommand command);

    CustomerInvoiceResponse createCreditNoteFromInvoice(UUID invoiceId, @Valid CreateCreditNoteFromInvoiceCommand command);

    CustomerInvoiceResponse postCustomerInvoice(UUID invoiceId);

    CustomerInvoiceResponse getCustomerInvoice(UUID invoiceId);

    List<CustomerInvoiceResponse> listCustomerInvoices(UUID companyId);

    List<CustomerPaymentResponse> listCustomerPayments(UUID companyId);

    CustomerPaymentResponse registerCustomerPayment(@Valid RegisterCustomerPaymentCommand command);
}
