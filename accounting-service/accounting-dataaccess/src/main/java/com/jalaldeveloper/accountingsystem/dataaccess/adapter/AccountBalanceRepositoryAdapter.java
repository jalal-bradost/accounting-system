package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountBalanceRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalItemJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AccountBalanceRepositoryAdapter implements AccountBalanceRepository {

    private final JournalItemJpaRepository journalItemJpaRepository;

    public AccountBalanceRepositoryAdapter(JournalItemJpaRepository journalItemJpaRepository) {
        this.journalItemJpaRepository = journalItemJpaRepository;
    }

    @Override
    public List<AccountBalanceLine> getTrialBalance(CompanyId companyId, LocalDate from, LocalDate to) {
        List<Object[]> rows = journalItemJpaRepository.findTrialBalance(companyId.getId(), from, to);
        return rows.stream()
                .map(row -> new AccountBalanceLine(
                        (UUID) row[0],
                        row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }
}
