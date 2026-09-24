package com.bradox.delin.dataaccess.adapter;

import com.bradox.delin.accounting.service.domain.ports.output.repository.AccountBalanceRepository;
import com.bradox.delin.dataaccess.repository.JournalItemJpaRepository;
import com.bradox.delin.domain.core.ValueObject.AccountType;
import com.bradox.delin.domain.valueobject.CompanyId;
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
        List<Object[]> rows = journalItemJpaRepository.findTrialBalance(
                companyId.getId(), from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return rows.stream()
                .map(row -> new AccountBalanceLine(
                        (UUID) row[0],
                        row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountBalanceLine> getBalancesUpTo(CompanyId companyId, LocalDate asOf, List<AccountType> accountTypes) {
        List<Object[]> rows = journalItemJpaRepository.findBalancesUpTo(
                companyId.getId(), asOf.plusDays(1).atStartOfDay(), accountTypes);
        return rows.stream()
                .map(row -> new AccountBalanceLine(
                        (UUID) row[0],
                        row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }
}
