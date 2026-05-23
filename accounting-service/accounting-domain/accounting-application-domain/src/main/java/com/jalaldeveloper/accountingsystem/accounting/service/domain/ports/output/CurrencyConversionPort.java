package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Resolves company-scoped FX rates from the currency master.
 * {@code exchangeRateToCompany} multiplies a document-currency amount to obtain company currency.
 */
public interface CurrencyConversionPort {

  /** Multiplier from document currency to company base (1 for base currency). */
  BigDecimal exchangeRateToCompany(UUID companyId, String currencyCode, LocalDate asOf);

  BigDecimal convertToCompany(
      UUID companyId, BigDecimal documentAmount, String currencyCode, LocalDate asOf);

  String baseCurrencyCode(UUID companyId);
}
