package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.CurrencyMath;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.CurrencyRow;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class CurrencyConversionAdapter implements CurrencyConversionPort {

  private final CompanyCurrencyRepository companyCurrencyRepository;

  public CurrencyConversionAdapter(CompanyCurrencyRepository companyCurrencyRepository) {
    this.companyCurrencyRepository = companyCurrencyRepository;
  }

  @Override
  public BigDecimal exchangeRateToCompany(UUID companyId, String currencyCode, LocalDate asOf) {
    CompanyId cid = new CompanyId(companyId);
    String code = normalizeCode(currencyCode);
  CurrencyRow row =
        companyCurrencyRepository
            .findByCompanyAndCode(cid, code)
            .orElseThrow(
                () ->
                    new AccountingDomainException(
                        "Company currency not configured: " + code));
    if (row.baseCurrency()) {
      return BigDecimal.ONE;
    }
    LocalDate date = asOf != null ? asOf : LocalDate.now();
    BigDecimal ratePerBase =
        companyCurrencyRepository
            .findEffectiveRate(row.id(), date)
            .map(CompanyCurrencyRepository.RateLine::rate)
            .orElse(row.ratePerBase());
    return CurrencyMath.ratePerBaseToExchangeRate(ratePerBase);
  }

  @Override
  public BigDecimal convertToCompany(
      UUID companyId, BigDecimal documentAmount, String currencyCode, LocalDate asOf) {
    BigDecimal rate = exchangeRateToCompany(companyId, currencyCode, asOf);
    return CurrencyMath.convertAtRate(documentAmount, rate);
  }

  @Override
  public String baseCurrencyCode(UUID companyId) {
    return companyCurrencyRepository
        .findBaseCurrency(new CompanyId(companyId))
        .map(CurrencyRow::code)
        .orElse("USD");
  }

  private static String normalizeCode(String currencyCode) {
    if (currencyCode == null || currencyCode.isBlank()) {
      throw new AccountingDomainException("Currency code is required");
    }
    return currencyCode.trim().toUpperCase();
  }
}
