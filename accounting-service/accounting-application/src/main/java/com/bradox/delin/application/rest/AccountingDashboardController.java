package com.bradox.delin.application.rest;

import com.bradox.delin.accounting.service.domain.AccountingDashboardService;
import com.bradox.delin.accounting.service.domain.dashboard.AccountingDashboardResponse;
import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.platform.security.RequiresPermission;
import com.bradox.delin.platform.web.CurrentCompany;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(value = "/api/v1/accounting", produces = "application/json")
public class AccountingDashboardController {

    private final AccountingDashboardService accountingDashboardService;

    public AccountingDashboardController(AccountingDashboardService accountingDashboardService) {
        this.accountingDashboardService = accountingDashboardService;
    }

    @GetMapping("/dashboard")
    @RequiresPermission(
            value = {"accounting.customer-invoice.read", "accounting.vendor-bill.read"},
            op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<AccountingDashboardResponse> dashboard(
            @CurrentCompany CompanyId companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(accountingDashboardService.getDashboard(companyId.getId(), from, to));
    }
}
