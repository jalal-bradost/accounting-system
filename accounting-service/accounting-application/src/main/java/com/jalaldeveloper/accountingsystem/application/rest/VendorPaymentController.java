package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.RegisterVendorPaymentCommand;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.VendorPaymentResponse;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.input.PurchaseApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
