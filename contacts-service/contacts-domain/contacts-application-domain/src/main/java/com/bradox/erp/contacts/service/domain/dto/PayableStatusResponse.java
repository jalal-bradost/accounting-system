package com.bradox.erp.contacts.service.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PayableStatusResponse(UUID partnerId,
                                    BigDecimal outstandingPayable,
                                    String companyCurrencyCode) {}
