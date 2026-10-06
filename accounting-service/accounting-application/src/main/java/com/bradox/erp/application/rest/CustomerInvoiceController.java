package com.bradox.erp.application.rest;

import com.bradox.erp.accounting.service.domain.customerinvoice.CreateCreditNoteFromInvoiceCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CreateCustomerInvoiceCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import com.bradox.erp.platform.application.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/search")
    @RequiresPermission("accounting.customer-invoice.read")
    public ResponseEntity<PageResponse<CustomerInvoiceResponse>> search(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) UUID salesOrderId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = customerInvoiceApplicationService.searchCustomerInvoices(
                companyId.getId(), state, salesOrderId, q, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result, java.util.function.Function.identity()));
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

    @GetMapping("/{id}/credit-notes")
    @RequiresPermission("accounting.customer-invoice.read")
    public ResponseEntity<List<CustomerInvoiceResponse>> listCreditNotes(@PathVariable UUID id) {
        return ResponseEntity.ok(customerInvoiceApplicationService.listCreditNotesForInvoice(id));
    }

    @PostMapping("/{id}/credit-note")
    @RequiresPermission("accounting.customer-invoice.write")
    public ResponseEntity<CustomerInvoiceResponse> createCreditNote(@CurrentCompany CompanyId companyId,
                                                                    @PathVariable UUID id,
                                                                    @Valid @RequestBody CreateCreditNoteFromInvoiceCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(customerInvoiceApplicationService.createCreditNoteFromInvoice(id, cmd));
    }

    @PostMapping("/{id}/credit/refund")
    @RequiresPermission("accounting.customer-payment.register")
    public ResponseEntity<CustomerPaymentResponse> refundCredit(
            @PathVariable UUID id,
            @RequestBody com.bradox.erp.accounting.service.domain.customerinvoice.RefundCustomerCreditCommand cmd) {
        return ResponseEntity.ok(customerInvoiceApplicationService.refundCustomerCredit(id, cmd));
    }

    @PostMapping("/{id}/credit/keep")
    @RequiresPermission("accounting.customer-payment.register")
    public ResponseEntity<java.util.Map<String, java.math.BigDecimal>> keepCredit(@PathVariable UUID id) {
        return ResponseEntity.ok(java.util.Map.of("keptAmount", customerInvoiceApplicationService.keepCustomerCredit(id)));
    }

    @PostMapping("/{id}/apply-credit")
    @RequiresPermission("accounting.customer-payment.register")
    public ResponseEntity<java.util.Map<String, java.math.BigDecimal>> applyCredit(@PathVariable UUID id) {
        return ResponseEntity.ok(java.util.Map.of("appliedAmount", customerInvoiceApplicationService.applyCustomerCredit(id)));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("accounting.customer-invoice.write")
    public ResponseEntity<Void> deleteDraft(@PathVariable UUID id) {
        customerInvoiceApplicationService.deleteDraftCustomerInvoice(id);
        return ResponseEntity.noContent().build();
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
