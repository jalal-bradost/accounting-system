package com.bradox.delin.application.rest;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.platform.security.RequiresPermission;
import com.bradox.delin.platform.web.CurrentCompany;
import com.bradox.delin.purchase.service.domain.dto.RegisterVendorPaymentCommand;
import com.bradox.delin.purchase.service.domain.dto.VendorPaymentResponse;
import com.bradox.delin.purchase.service.domain.ports.input.PurchaseApplicationService;
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
@RequestMapping(value = "/api/v1/accounting/vendor-payments", produces = "application/json")
public class VendorPaymentController {

    private final PurchaseApplicationService purchaseApplicationService;

    public VendorPaymentController(PurchaseApplicationService purchaseApplicationService) {
        this.purchaseApplicationService = purchaseApplicationService;
    }

    @GetMapping
    @RequiresPermission("accounting.vendor-bill.read")
    public ResponseEntity<List<VendorPaymentResponse>> list(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(purchaseApplicationService.listVendorPayments(companyId.getId()));
    }

    @PostMapping
    @RequiresPermission("accounting.vendor-payment.register")
    public ResponseEntity<VendorPaymentResponse> register(@CurrentCompany CompanyId companyId,
                                                          @Valid @RequestBody RegisterVendorPaymentCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.registerVendorPayment(cmd));
    }

    @PostMapping("/{id}/reverse")
    @RequiresPermission("accounting.vendor-payment.register")
    public ResponseEntity<VendorPaymentResponse> reverse(@PathVariable UUID id,
                                                         @RequestBody(required = false) ReversePaymentRequest body) {
        String reason = body != null ? body.reason() : null;
        return ResponseEntity.ok(purchaseApplicationService.reverseVendorPayment(id, reason));
    }

    public record ReversePaymentRequest(String reason) {}
}
