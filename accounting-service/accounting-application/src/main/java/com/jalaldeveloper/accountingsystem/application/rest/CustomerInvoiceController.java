package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CreateCreditNoteFromInvoiceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CreateCustomerInvoiceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/accounting/customer-invoices", produces = "application/json")
public class CustomerInvoiceController {

    private final CustomerInvoiceApplicationService customerInvoiceApplicationService;

    public CustomerInvoiceController(CustomerInvoiceApplicationService customerInvoiceApplicationService) {
        this.customerInvoiceApplicationService = customerInvoiceApplicationService;
    }

    @PostMapping
    @RequiresPermission("accounting.customer-invoice.write")
    public ResponseEntity<CustomerInvoiceResponse> create(@CurrentCompany CompanyId companyId,
                                                         @Valid @RequestBody CreateCustomerInvoiceCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(customerInvoiceApplicationService.createCustomerInvoice(cmd));
    }

    @GetMapping
    @RequiresPermission("accounting.customer-invoice.read")
    public ResponseEntity<List<CustomerInvoiceResponse>> list(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(customerInvoiceApplicationService.listCustomerInvoices(companyId.getId()));
    }

    @GetMapping("/payments")
    @RequiresPermission("accounting.customer-invoice.read")
    public ResponseEntity<List<CustomerPaymentResponse>> listPayments(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(customerInvoiceApplicationService.listCustomerPayments(companyId.getId()));
    }

    @GetMapping("/{id}")
    @RequiresPermission("accounting.customer-invoice.read")
    public ResponseEntity<CustomerInvoiceResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(customerInvoiceApplicationService.getCustomerInvoice(id));
    }

    @PostMapping("/{id}/credit-note")
    @RequiresPermission("accounting.customer-invoice.write")
    public ResponseEntity<CustomerInvoiceResponse> createCreditNote(@CurrentCompany CompanyId companyId,
                                                                    @PathVariable UUID id,
                                                                    @Valid @RequestBody CreateCreditNoteFromInvoiceCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(customerInvoiceApplicationService.createCreditNoteFromInvoice(id, cmd));
    }

    @PostMapping("/{id}/post")
    @RequiresPermission("accounting.customer-invoice.post")
    public ResponseEntity<CustomerInvoiceResponse> post(@PathVariable UUID id) {
        return ResponseEntity.ok(customerInvoiceApplicationService.postCustomerInvoice(id));
    }

    @PostMapping("/payments")
    @RequiresPermission("accounting.customer-payment.register")
    public ResponseEntity<CustomerPaymentResponse> registerPayment(@CurrentCompany CompanyId companyId,
                                                                   @Valid @RequestBody RegisterCustomerPaymentCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(customerInvoiceApplicationService.registerCustomerPayment(cmd));
    }
}
