package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.GeneralLedgerRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalItemJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GeneralLedgerRepositoryAdapter implements GeneralLedgerRepository {

    private final JournalItemJpaRepository journalItemJpaRepository;

    public GeneralLedgerRepositoryAdapter(JournalItemJpaRepository journalItemJpaRepository) {
        this.journalItemJpaRepository = journalItemJpaRepository;
    }

    @Override
    public List<GeneralLedgerRawLine> listPostedLines(CompanyId companyId, LocalDate from, LocalDate to, UUID accountId) {
        List<Object[]> rows = journalItemJpaRepository.findGeneralLedgerLines(companyId.getId(), from, to, accountId);
        return rows.stream()
                .map(row -> new GeneralLedgerRawLine(
                        (UUID) row[0],
                        (UUID) row[1],
                        (LocalDate) row[2],
                        (String) row[3],
                        (String) row[4],
                        row[5] != null ? (String) row[5] : "",
                        row[6] != null ? (BigDecimal) row[6] : BigDecimal.ZERO,
                        row[7] != null ? (BigDecimal) row[7] : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }
}
