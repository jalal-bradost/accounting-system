package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll;

import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.PayRun;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Payslip;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayRunRepository {

    PayRun save(PayRun payRun);

    Optional<PayRun> findById(UUID id);

    List<PayRun> listByCompany(UUID companyId);

    Optional<Payslip> findPayslipById(UUID payslipId);
}
