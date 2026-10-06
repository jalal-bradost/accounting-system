package com.bradox.erp.application.rest;

import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.accounting.service.domain.ports.output.CustomerBalanceQueryPort;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * What customers owe, in the company currency: a positive balance is money the customer still owes.
 * Used by the salesperson workspace to show each customer's account at a glance.
 */
@RestController
@RequestMapping(value = "/api/v1/accounting/customer-balances", produces = "application/json")
public class CustomerBalanceController {

    private final CustomerBalanceQueryPort queryPort;
    private final CurrencyConversionPort currencyConversionPort;

    public CustomerBalanceController(CustomerBalanceQueryPort queryPort, CurrencyConversionPort currencyConversionPort) {
        this.queryPort = queryPort;
        this.currencyConversionPort = currencyConversionPort;
    }

    public record Row(UUID partnerId, BigDecimal balance) {}

    public record Response(String currencyCode, List<Row> balances) {}

    @GetMapping
    @RequiresPermission("accounting.customer-invoice.read")
    @Transactional(readOnly = true)
    public ResponseEntity<Response> balances(@CurrentCompany CompanyId companyId,
                                             @RequestParam(name = "partnerId", required = false) List<UUID> partnerIds) {
        List<Row> rows = queryPort.findBalances(companyId.getId(), partnerIds).stream()
                .map(b -> new Row(b.partnerId(), b.balance()))
                .toList();
        return ResponseEntity.ok(new Response(currencyConversionPort.baseCurrencyCode(companyId.getId()), rows));
    }
}
