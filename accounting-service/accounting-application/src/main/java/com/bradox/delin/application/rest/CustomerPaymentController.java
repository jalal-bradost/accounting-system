package com.bradox.delin.application.rest;

import com.bradox.delin.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.bradox.delin.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.bradox.delin.platform.security.RequiresPermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/accounting/customer-payments", produces = "application/json")
public class CustomerPaymentController {

    private final CustomerInvoiceApplicationService customerInvoiceApplicationService;

    public CustomerPaymentController(CustomerInvoiceApplicationService customerInvoiceApplicationService) {
        this.customerInvoiceApplicationService = customerInvoiceApplicationService;
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
