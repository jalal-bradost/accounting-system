package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CreateCustomerInvoiceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface CustomerInvoiceApplicationService {

    boolean hasPostedInvoiceForSalesOrder(UUID salesOrderId);

    CustomerInvoiceResponse createCustomerInvoice(@Valid CreateCustomerInvoiceCommand command);

    CustomerInvoiceResponse postCustomerInvoice(UUID invoiceId);

    CustomerInvoiceResponse getCustomerInvoice(UUID invoiceId);

    List<CustomerInvoiceResponse> listCustomerInvoices(UUID companyId);

    List<CustomerPaymentResponse> listCustomerPayments(UUID companyId);

    CustomerPaymentResponse registerCustomerPayment(@Valid RegisterCustomerPaymentCommand command);
}
