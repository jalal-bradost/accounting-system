package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReportingApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountBalanceRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ReportingApplicationServiceImpl implements ReportingApplicationService {

    private final AccountBalanceRepository accountBalanceRepository;

    public ReportingApplicationServiceImpl(AccountBalanceRepository accountBalanceRepository) {
        this.accountBalanceRepository = accountBalanceRepository;
    }

    @Override
    public List<AccountBalanceRepository.AccountBalanceLine> getTrialBalance(UUID companyId, LocalDate from, LocalDate to) {
        return accountBalanceRepository.getTrialBalance(new CompanyId(companyId), from, to);
    }
}
