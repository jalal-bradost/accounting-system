package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement.PartnerStatementResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface PartnerStatementApplicationService {

    PartnerStatementResponse partnerStatement(UUID companyId, UUID partnerId, LocalDate from, LocalDate to);
}
