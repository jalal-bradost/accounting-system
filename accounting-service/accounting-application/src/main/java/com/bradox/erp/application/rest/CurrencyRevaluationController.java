package com.bradox.erp.application.rest;

import com.bradox.erp.accounting.service.domain.revaluation.CurrencyRevaluationResult;
import com.bradox.erp.accounting.service.domain.revaluation.CurrencyRevaluationService;
import com.bradox.erp.platform.security.RequiresPermission;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Period-end revaluation of open foreign-currency receivables and payables. {@code preview=true}
 * (the default) only reports what would be booked.
 */
@RestController
@RequestMapping(value = "/api/v1/companies/{companyId}/currency-revaluation", produces = "application/json")
public class CurrencyRevaluationController {

    private final CurrencyRevaluationService service;

    public CurrencyRevaluationController(CurrencyRevaluationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("accounting.currency.write")
    public ResponseEntity<CurrencyRevaluationResult> revalue(
            @PathVariable UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam(defaultValue = "true") boolean preview) {
        return ResponseEntity.ok(preview ? service.preview(companyId, asOf) : service.post(companyId, asOf));
    }
}
