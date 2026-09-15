package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.AccountingDashboardService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.dashboard.AccountingDashboardResponse;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
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
