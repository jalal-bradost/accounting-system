package com.bradox.erp.repair.service.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateLineCommand(String sectionLabel, Integer sequence, String description, BigDecimal qty, BigDecimal unitPrice,
                                BigDecimal discountPercent, UUID vendorPartnerId, BigDecimal vendorCost) {
}
