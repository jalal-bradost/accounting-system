package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement.PartnerStatementResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.PartnerStatementApplicationService;
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
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/accounting/partner-statement", produces = "application/json")
public class PartnerStatementController {

    private final PartnerStatementApplicationService partnerStatementApplicationService;

    public PartnerStatementController(PartnerStatementApplicationService partnerStatementApplicationService) {
        this.partnerStatementApplicationService = partnerStatementApplicationService;
    }

    @GetMapping
    @RequiresPermission(value = {"accounting.customer-invoice.read", "accounting.vendor-bill.read"},
            op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<PartnerStatementResponse> get(@CurrentCompany CompanyId companyId,
                                                        @RequestParam UUID partnerId,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                        LocalDate from,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                        LocalDate to) {
        return ResponseEntity.ok(
                partnerStatementApplicationService.partnerStatement(companyId.getId(), partnerId, from, to));
    }
}
