package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.dto.ProfitabilityResponse;
import com.bradox.erp.timesheet.service.domain.dto.RecomputeCostResponse;

import java.util.UUID;

public interface CostApplicationService {

    /** Needs {@code tsh.cost.view} (BR-TSH-10). */
    ProfitabilityResponse profitability(CompanyId companyId, UUID projectId);

    /** TSH-07 #6, #7: prices only the entries that were flagged "no cost rate". */
    RecomputeCostResponse recomputeMissing(CompanyId companyId);
}
