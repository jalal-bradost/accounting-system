package com.jalaldeveloper.accountingsystem.application.currency;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CompanyCurrencyApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.CurrencyRow;
import com.jalaldeveloper.accountingsystem.platform.settings.BaseCurrencyChangeHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

/**
 * Accounting-side implementation of the platform hook. Re-points the company's base
 * currency to match its default currency, but refuses once ledger activity exists so
 * historical amounts keep their meaning.
 */
@Component
public class BaseCurrencyChangeHandlerImpl implements BaseCurrencyChangeHandler {

    private final CompanyCurrencyApplicationService companyCurrencyApplicationService;

    public BaseCurrencyChangeHandlerImpl(
            CompanyCurrencyApplicationService companyCurrencyApplicationService) {
        this.companyCurrencyApplicationService = companyCurrencyApplicationService;
    }

    @Override
    public void changeBaseCurrency(UUID companyId, String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return;
        }
        String normalized = currencyCode.trim().toUpperCase();

        Optional<CurrencyRow> base = companyCurrencyApplicationService.baseCurrency(companyId);
        if (base.isPresent() && base.get().code().equalsIgnoreCase(normalized)) {
            return; // Already aligned — nothing to do.
        }

        if (companyCurrencyApplicationService.baseCurrencyLocked(companyId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Base currency cannot be changed once the company has transactions.");
        }

        companyCurrencyApplicationService.setBaseCurrency(companyId, normalized);
    }
}
