package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared FX math: document amount × rateToCompany = company (base) amount. */
public final class CurrencyMath {

  private CurrencyMath() {}

  public static BigDecimal convertAtRate(BigDecimal documentAmount, BigDecimal rateToCompany) {
    BigDecimal r = rateToCompany != null && rateToCompany.signum() > 0 ? rateToCompany : BigDecimal.ONE;
    return documentAmount.multiply(r).setScale(4, RoundingMode.HALF_UP);
  }

  /** {@code ratePerBase} = units of foreign currency per 1 base unit (e.g. IQD per USD). */
  public static BigDecimal ratePerBaseToExchangeRate(BigDecimal ratePerBase) {
    if (ratePerBase == null || ratePerBase.signum() <= 0) {
      return BigDecimal.ONE;
    }
    return BigDecimal.ONE.divide(ratePerBase, 12, RoundingMode.HALF_UP);
  }
}
