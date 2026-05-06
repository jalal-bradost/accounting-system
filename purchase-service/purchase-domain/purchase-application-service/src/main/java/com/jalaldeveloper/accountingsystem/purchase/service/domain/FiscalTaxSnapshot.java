package com.jalaldeveloper.accountingsystem.purchase.service.domain;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.TaxAmountType;

import java.math.BigDecimal;
import java.util.UUID;

public record FiscalTaxSnapshot(UUID id, TaxAmountType amountType, BigDecimal amount, boolean priceInclude) {}
