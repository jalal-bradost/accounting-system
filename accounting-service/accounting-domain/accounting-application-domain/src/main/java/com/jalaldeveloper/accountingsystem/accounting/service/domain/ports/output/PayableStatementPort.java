package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement.PartnerStatementSectionResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Output port for vendor payable statement sections without depending on the purchase module at the Maven level.
 */
public interface PayableStatementPort {

    List<PartnerStatementSectionResponse> payableStatement(UUID companyId, UUID partnerId, LocalDate from, LocalDate to);
}
