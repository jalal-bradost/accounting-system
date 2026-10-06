package com.bradox.erp.sales.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.sales.service.domain.SalesConsistencyService;
import com.bradox.erp.sales.service.domain.ports.output.SalesConsistencyQueryPort.Issue;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Read-only report of places where orders, deliveries, invoices and the ledger disagree. */
@RestController
@RequestMapping(value = "/api/v1/sales", produces = "application/json")
public class SalesConsistencyController {

    private final SalesConsistencyService consistencyService;

    public SalesConsistencyController(SalesConsistencyService consistencyService) {
        this.consistencyService = consistencyService;
    }

    @GetMapping("/consistency")
    @RequiresPermission("sales.order.read")
    public ResponseEntity<List<Issue>> check(@CurrentCompany CompanyId companyId,
                                             @RequestParam(required = false) UUID salesOrderId) {
        return ResponseEntity.ok(consistencyService.check(companyId.getId(), salesOrderId));
    }
}
