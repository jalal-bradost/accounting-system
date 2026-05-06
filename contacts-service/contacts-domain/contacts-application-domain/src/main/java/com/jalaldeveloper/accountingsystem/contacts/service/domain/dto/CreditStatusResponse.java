package com.jalaldeveloper.accountingsystem.contacts.service.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditStatusResponse(UUID partnerId,
                                   BigDecimal creditLimit,
                                   BigDecimal outstandingReceivable,
                                   BigDecimal available,
                                   boolean unlimited) {}
