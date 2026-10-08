package com.bradox.erp.timesheet.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.timesheet.service.domain.dto.AssignLineCommand;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;
import com.bradox.erp.timesheet.service.domain.dto.NeedsAttentionItem;
import com.bradox.erp.timesheet.service.domain.dto.RateResponse;
import com.bradox.erp.timesheet.service.domain.dto.SaleLineResponse;
import com.bradox.erp.timesheet.service.domain.dto.SetRatesCommand;
import com.bradox.erp.timesheet.service.domain.ports.input.BillingApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Billing hours to customers through sales order lines (TSH-06). */
@RestController
@RequestMapping(value = "/api/v1/timesheet", produces = "application/json")
public class BillingController {

    private final BillingApplicationService billing;

    public BillingController(BillingApplicationService billing) {
        this.billing = billing;
    }

    @GetMapping("/billing/sale-lines")
    @RequiresPermission("tsh.entry.own")
    public List<SaleLineResponse> saleLines(@CurrentCompany CompanyId companyId, @RequestParam(required = false) UUID customerId) {
        return billing.eligibleLines(companyId, customerId);
    }

    @GetMapping("/billing/needs-attention")
    @RequiresPermission("tsh.billing.manage")
    public List<NeedsAttentionItem> needsAttention(@CurrentCompany CompanyId companyId) {
        return billing.needsAttention(companyId);
    }

    @GetMapping("/projects/{id}/rates")
    @RequiresPermission("tsh.entry.own")
    public List<RateResponse> rates(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return billing.rates(companyId, id);
    }

    @PutMapping("/projects/{id}/rates")
    @RequiresPermission("tsh.billing.manage")
    public List<RateResponse> setRates(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                       @Valid @RequestBody SetRatesCommand command) {
        return billing.setRates(companyId, id, command);
    }

    @PostMapping("/entries/{id}/assign-line")
    @RequiresPermission("tsh.billing.manage")
    public EntryResponse assignLine(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                    @Valid @RequestBody AssignLineCommand command) {
        return billing.assignLine(companyId, id, command.saleLineId());
    }
}
