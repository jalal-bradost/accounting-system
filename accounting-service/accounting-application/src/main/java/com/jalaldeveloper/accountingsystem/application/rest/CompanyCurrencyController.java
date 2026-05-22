package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CompanyCurrencyApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.CurrencyRow;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.PageResult;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.RateLine;
import com.jalaldeveloper.accountingsystem.platform.application.dto.PageResponse;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/companies/{companyId}/currencies", produces = "application/json")
public class CompanyCurrencyController {

    private final CompanyCurrencyApplicationService companyCurrencyApplicationService;

    public CompanyCurrencyController(CompanyCurrencyApplicationService companyCurrencyApplicationService) {
        this.companyCurrencyApplicationService = companyCurrencyApplicationService;
    }

    @GetMapping
    @RequiresPermission("accounting.currency.read")
    public ResponseEntity<PageResponse<CurrencyResponse>> list(
            @PathVariable UUID companyId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult result = companyCurrencyApplicationService.listCurrencies(companyId, q, page, size);
        PageResponse<CurrencyResponse> body =
                new PageResponse<>(
                        result.content().stream().map(CurrencyResponse::from).toList(),
                        result.page(),
                        result.size(),
                        result.totalElements(),
                        result.totalPages());
        return ResponseEntity.ok(body);
    }

    @PostMapping
    @RequiresPermission("accounting.currency.write")
    public ResponseEntity<CurrencyResponse> create(
            @PathVariable UUID companyId, @Valid @RequestBody CreateCompanyCurrencyRequest request) {
        CurrencyRow row =
                companyCurrencyApplicationService.createCurrency(
                        companyId,
                        request.getCode(),
                        request.getSymbol(),
                        request.getName(),
                        request.getRatePerBase(),
                        request.isActive());
        return ResponseEntity.ok(CurrencyResponse.from(row));
    }

    @PutMapping("/{id}")
    @RequiresPermission("accounting.currency.write")
    public ResponseEntity<CurrencyResponse> update(
            @PathVariable UUID companyId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyCurrencyRequest request) {
        CurrencyRow row =
                companyCurrencyApplicationService.updateCurrency(
                        companyId,
                        id,
                        request.getSymbol(),
                        request.getName(),
                        request.getRatePerBase(),
                        request.isActive());
        return ResponseEntity.ok(CurrencyResponse.from(row));
    }

    @GetMapping("/{id}/rates")
    @RequiresPermission("accounting.currency.read")
    public ResponseEntity<List<CurrencyRateResponse>> listRates(
            @PathVariable UUID companyId, @PathVariable UUID id) {
        List<CurrencyRateResponse> rates =
                companyCurrencyApplicationService.listRates(companyId, id).stream()
                        .map(CurrencyRateResponse::from)
                        .toList();
        return ResponseEntity.ok(rates);
    }

    @PostMapping("/{id}/rates")
    @RequiresPermission("accounting.currency.write")
    public ResponseEntity<CurrencyRateResponse> addRate(
            @PathVariable UUID companyId,
            @PathVariable UUID id,
            @Valid @RequestBody AddCurrencyRateRequest request) {
        RateLine line =
                companyCurrencyApplicationService.addRate(
                        companyId, id, request.getEffectiveDate(), request.getRate());
        return ResponseEntity.ok(CurrencyRateResponse.from(line));
    }

    @GetMapping("/{id}/rates/effective")
    @RequiresPermission("accounting.currency.read")
    public ResponseEntity<CurrencyRateResponse> effectiveRate(
            @PathVariable UUID companyId,
            @PathVariable UUID id,
            @RequestParam("date") LocalDate date) {
        return companyCurrencyApplicationService
                .rateOn(companyId, id, date)
                .map(line -> ResponseEntity.ok(CurrencyRateResponse.from(line)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public static final class CurrencyResponse {
        private final UUID id;
        private final String code;
        private final String symbol;
        private final String name;
        private final LocalDate lastRateUpdated;
        private final BigDecimal ratePerBase;
        private final boolean baseCurrency;
        private final boolean active;

        private CurrencyResponse(
                UUID id,
                String code,
                String symbol,
                String name,
                LocalDate lastRateUpdated,
                BigDecimal ratePerBase,
                boolean baseCurrency,
                boolean active) {
            this.id = id;
            this.code = code;
            this.symbol = symbol;
            this.name = name;
            this.lastRateUpdated = lastRateUpdated;
            this.ratePerBase = ratePerBase;
            this.baseCurrency = baseCurrency;
            this.active = active;
        }

        static CurrencyResponse from(CurrencyRow r) {
            return new CurrencyResponse(
                    r.id(),
                    r.code(),
                    r.symbol(),
                    r.name(),
                    r.lastRateUpdated(),
                    r.ratePerBase(),
                    r.baseCurrency(),
                    r.active());
        }

        public UUID getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getName() {
            return name;
        }

        public LocalDate getLastRateUpdated() {
            return lastRateUpdated;
        }

        public BigDecimal getRatePerBase() {
            return ratePerBase;
        }

        public boolean isBaseCurrency() {
            return baseCurrency;
        }

        public boolean isActive() {
            return active;
        }
    }

    public static class CreateCompanyCurrencyRequest {
        @NotBlank
        @Size(min = 3, max = 3)
        private String code;
        @NotBlank
        @Size(max = 16)
        private String symbol;
        @NotBlank
        @Size(max = 200)
        private String name;
        @NotNull private BigDecimal ratePerBase;
        private boolean active = true;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getRatePerBase() {
            return ratePerBase;
        }

        public void setRatePerBase(BigDecimal ratePerBase) {
            this.ratePerBase = ratePerBase;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    public static final class CurrencyRateResponse {
        private final UUID id;
        private final UUID currencyId;
        private final LocalDate effectiveDate;
        private final BigDecimal rate;

        private CurrencyRateResponse(UUID id, UUID currencyId, LocalDate effectiveDate, BigDecimal rate) {
            this.id = id;
            this.currencyId = currencyId;
            this.effectiveDate = effectiveDate;
            this.rate = rate;
        }

        static CurrencyRateResponse from(RateLine line) {
            return new CurrencyRateResponse(
                    line.id(), line.currencyId(), line.effectiveDate(), line.rate());
        }

        public UUID getId() {
            return id;
        }

        public UUID getCurrencyId() {
            return currencyId;
        }

        public LocalDate getEffectiveDate() {
            return effectiveDate;
        }

        public BigDecimal getRate() {
            return rate;
        }
    }

    public static class AddCurrencyRateRequest {
        @NotNull private LocalDate effectiveDate;
        @NotNull private BigDecimal rate;

        public LocalDate getEffectiveDate() {
            return effectiveDate;
        }

        public void setEffectiveDate(LocalDate effectiveDate) {
            this.effectiveDate = effectiveDate;
        }

        public BigDecimal getRate() {
            return rate;
        }

        public void setRate(BigDecimal rate) {
            this.rate = rate;
        }
    }

    public static class UpdateCompanyCurrencyRequest {
        @NotBlank
        @Size(max = 16)
        private String symbol;
        @NotBlank
        @Size(max = 200)
        private String name;
        @NotNull private BigDecimal ratePerBase;
        private boolean active;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getRatePerBase() {
            return ratePerBase;
        }

        public void setRatePerBase(BigDecimal ratePerBase) {
            this.ratePerBase = ratePerBase;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
