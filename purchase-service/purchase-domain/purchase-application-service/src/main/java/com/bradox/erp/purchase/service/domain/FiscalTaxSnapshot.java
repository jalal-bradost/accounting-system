package com.bradox.erp.purchase.service.domain;

import com.bradox.erp.purchase.domain.core.TaxAmountType;

import java.math.BigDecimal;
import java.util.UUID;

public record FiscalTaxSnapshot(UUID id, TaxAmountType amountType, BigDecimal amount, boolean priceInclude) {}
