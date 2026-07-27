package com.jalaldeveloper.accountingsystem.hr.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PayRunEntity;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.mapper.PayRunDataAccessMapper;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.repository.PayPayslipJpaRepository;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.repository.PayRunJpaRepository;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.PayRun;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Payslip;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll.PayRunRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PayRunRepositoryImpl implements PayRunRepository {

    private final PayRunJpaRepository payRunJpaRepository;
    private final PayPayslipJpaRepository payslipJpaRepository;
    private final PayRunDataAccessMapper mapper;

    public PayRunRepositoryImpl(PayRunJpaRepository payRunJpaRepository,
                                PayPayslipJpaRepository payslipJpaRepository,
                                PayRunDataAccessMapper mapper) {
        this.payRunJpaRepository = payRunJpaRepository;
        this.payslipJpaRepository = payslipJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PayRun save(PayRun payRun) {
        PayRunEntity existing = payRunJpaRepository.findById(payRun.getId()).orElse(null);
        PayRunEntity toSave = mapper.domainToEntity(payRun, existing);
        return mapper.entityToDomain(payRunJpaRepository.save(toSave));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PayRun> findById(UUID id) {
        return payRunJpaRepository.findById(id).map(mapper::entityToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayRun> listByCompany(UUID companyId) {
        return payRunJpaRepository.findByCompanyIdOrderByPeriodStartDescCreatedAtDesc(companyId).stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payslip> findPayslipById(UUID payslipId) {
        return payslipJpaRepository.findById(payslipId).map(mapper::payslipEntityToDomain);
    }
}
