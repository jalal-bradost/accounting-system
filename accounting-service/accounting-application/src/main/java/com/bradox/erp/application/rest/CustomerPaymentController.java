package com.bradox.erp.application.rest;

import com.bradox.erp.accounting.service.domain.customerinvoice.AllocateCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.application.dto.PageResponse;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/accounting/customer-payments", produces = "application/json")
public class CustomerPaymentController {

    private final CustomerInvoiceApplicationService customerInvoiceApplicationService;

    public CustomerPaymentController(CustomerInvoiceApplicationService customerInvoiceApplicationService) {
        this.customerInvoiceApplicationService = customerInvoiceApplicationService;
    }

    @GetMapping
    @RequiresPermission("accounting.customer-invoice.read")
    public ResponseEntity<List<CustomerPaymentResponse>> list(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(customerInvoiceApplicationService.listCustomerPayments(companyId.getId()));
    }

    @GetMapping("/search")
    @RequiresPermission("accounting.customer-invoice.read")
    public ResponseEntity<PageResponse<CustomerPaymentResponse>> search(
            @CurrentCompany CompanyId companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = customerInvoiceApplicationService.searchCustomerPayments(
                companyId.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result, Function.identity()));
    }

    @GetMapping("/{id}")
    @RequiresPermission("accounting.customer-invoice.read")
    public ResponseEntity<CustomerPaymentResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(customerInvoiceApplicationService.getCustomerPayment(id));
    }

    @PostMapping
    @RequiresPermission("accounting.customer-payment.register")
    public ResponseEntity<CustomerPaymentResponse> register(@CurrentCompany CompanyId companyId,
                                                            @Valid @RequestBody RegisterCustomerPaymentCommand cmd) {
        if (cmd.getCompanyId() == null) {
            cmd.setCompanyId(companyId.getId());
        }
        return ResponseEntity.ok(customerInvoiceApplicationService.registerCustomerPayment(cmd));
    }

    @PostMapping("/{id}/allocations")
    @RequiresPermission("accounting.customer-payment.register")
    public ResponseEntity<CustomerPaymentResponse> allocate(@PathVariable UUID id,
                                                            @Valid @RequestBody AllocateCustomerPaymentCommand cmd) {
        return ResponseEntity.ok(customerInvoiceApplicationService.allocateCustomerPayment(id, cmd));
    }

    @PostMapping("/allocations/{allocationId}/reverse")
    @RequiresPermission("accounting.customer-payment.register")
    public ResponseEntity<CustomerPaymentResponse> deallocate(@PathVariable UUID allocationId) {
        return ResponseEntity.ok(customerInvoiceApplicationService.deallocateCustomerPayment(allocationId));
    }

    @PostMapping("/{id}/correct")
    @RequiresPermission("accounting.customer-payment.register")
    public ResponseEntity<CustomerPaymentResponse> correct(
            @PathVariable UUID id,
            @RequestBody com.bradox.erp.accounting.service.domain.customerinvoice.CorrectCustomerPaymentCommand body) {
        return ResponseEntity.ok(customerInvoiceApplicationService.correctCustomerPayment(id, body));
    }

    @PostMapping("/{id}/reverse")
    @RequiresPermission("accounting.customer-payment.register")
    public ResponseEntity<CustomerPaymentResponse> reverse(@PathVariable UUID id,
                                                           @RequestBody(required = false) ReversePaymentRequest body) {
        String reason = body != null ? body.reason() : null;
        return ResponseEntity.ok(customerInvoiceApplicationService.reverseCustomerPayment(id, reason));
    }

    public record ReversePaymentRequest(String reason) {}
}
