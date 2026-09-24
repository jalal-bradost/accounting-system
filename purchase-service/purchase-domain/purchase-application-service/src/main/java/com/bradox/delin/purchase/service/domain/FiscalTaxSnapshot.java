package com.bradox.delin.purchase.service.domain;

import com.bradox.delin.purchase.domain.core.TaxAmountType;

import java.math.BigDecimal;
import java.util.UUID;

public record FiscalTaxSnapshot(UUID id, TaxAmountType amountType, BigDecimal amount, boolean priceInclude) {}
