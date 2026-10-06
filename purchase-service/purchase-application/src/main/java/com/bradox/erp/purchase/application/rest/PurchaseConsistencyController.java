package com.bradox.erp.purchase.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.purchase.service.domain.PurchaseConsistencyService;
import com.bradox.erp.purchase.service.domain.ports.output.PurchaseConsistencyQueryPort.Issue;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Read-only report of places where purchase orders, receipts and vendor bills disagree. */
@RestController
@RequestMapping(value = "/api/v1/purchase", produces = "application/json")
public class PurchaseConsistencyController {

    private final PurchaseConsistencyService consistencyService;

    public PurchaseConsistencyController(PurchaseConsistencyService consistencyService) {
        this.consistencyService = consistencyService;
    }

    @GetMapping("/consistency")
    @RequiresPermission("purchase.order.read")
    public ResponseEntity<List<Issue>> check(@CurrentCompany CompanyId companyId,
                                             @RequestParam(required = false) UUID purchaseOrderId) {
        return ResponseEntity.ok(consistencyService.check(companyId.getId(), purchaseOrderId));
    }
}
