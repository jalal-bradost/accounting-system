package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.payroll.PayrollApi.*;

import java.util.List;
import java.util.UUID;

public interface PayRunApplicationService {

    PayRunResponse createRun(CreatePayRunCommand cmd);

    PayRunResponse computeRun(UUID id);

    PayRunResponse getRun(UUID id);

    List<PayRunSummaryResponse> listRuns(CompanyId companyId);

    PayslipResponse getPayslip(UUID payslipId);

    PayRunResponse postRun(UUID id);

    PayRunResponse payRun(UUID id, PayRunCommand cmd);
}
