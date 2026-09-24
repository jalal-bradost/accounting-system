package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.repository.PartnerLedgerRepository;
import com.bradox.erp.dataaccess.repository.JournalEntryJpaRepository;
import com.bradox.erp.dataaccess.repository.JournalItemJpaRepository;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.core.ValueObject.JournalEntryStatus;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PartnerLedgerRepositoryAdapter implements PartnerLedgerRepository {

    private final JournalItemJpaRepository journalItemJpaRepository;
    private final JournalEntryJpaRepository journalEntryJpaRepository;

    public PartnerLedgerRepositoryAdapter(JournalItemJpaRepository journalItemJpaRepository,
                                          JournalEntryJpaRepository journalEntryJpaRepository) {
        this.journalItemJpaRepository = journalItemJpaRepository;
        this.journalEntryJpaRepository = journalEntryJpaRepository;
    }

    @Override
    public Map<UUID, BigDecimal> sumOpeningBalanceByPartnerBefore(
            CompanyId companyId,
            LocalDate fromExclusive,
            List<AccountType> tradeAccountTypes) {
        List<Object[]> rows = journalItemJpaRepository.sumOpeningBalanceByPartnerBefore(
                companyId.getId(), fromExclusive.atStartOfDay(), tradeAccountTypes);
        Map<UUID, BigDecimal> out = new HashMap<>();
        for (Object[] row : rows) {
            UUID pid = (UUID) row[0];
            BigDecimal sum = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            out.put(pid, sum);
        }
        return out;
    }

    @Override
    public Map<UUID, PeriodDebitCredit> sumPeriodDebitCreditByPartner(
            CompanyId companyId,
            LocalDate fromInclusive,
            LocalDate toInclusive,
            List<AccountType> tradeAccountTypes) {
        List<Object[]> rows = journalItemJpaRepository.sumPeriodDebitCreditByPartner(
                companyId.getId(),
                fromInclusive.atStartOfDay(),
                toInclusive.plusDays(1).atStartOfDay(),
                tradeAccountTypes);
        Map<UUID, PeriodDebitCredit> out = new HashMap<>();
        for (Object[] row : rows) {
            UUID pid = (UUID) row[0];
            BigDecimal d = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal c = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            out.put(pid, new PeriodDebitCredit(d, c));
        }
        return out;
    }

    @Override
    public List<PartnerLedgerMovementRaw> listMovementsInPeriod(
            CompanyId companyId,
            LocalDate fromInclusive,
            LocalDate toInclusive,
            UUID partnerId,
            List<AccountType> tradeAccountTypes) {
        List<Object[]> rows = journalItemJpaRepository.listPartnerTradeMovementsInPeriod(
                companyId.getId(),
                fromInclusive.atStartOfDay(),
                toInclusive.plusDays(1).atStartOfDay(),
                partnerId,
                tradeAccountTypes);
        List<PartnerLedgerMovementRaw> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            UUID pid = (UUID) row[0];
            String nameHint = row[1] != null ? (String) row[1] : null;
            UUID entryId = (UUID) row[2];
            LocalDateTime entryDate = (LocalDateTime) row[3];
            String journalCode = (String) row[4];
            String journalName = row[5] != null ? (String) row[5] : "";
            String seq = (String) row[6];
            String accountCode = (String) row[7];
            String accountName = (String) row[8];
            String label = row[9] != null ? (String) row[9] : "";
            BigDecimal debit = row[10] != null ? (BigDecimal) row[10] : BigDecimal.ZERO;
            BigDecimal credit = row[11] != null ? (BigDecimal) row[11] : BigDecimal.ZERO;
            UUID recon = (UUID) row[12];
            UUID itemId = (UUID) row[13];
            out.add(new PartnerLedgerMovementRaw(
                    pid, nameHint, entryId, entryDate, journalCode, journalName, seq, accountCode, accountName,
                    label, debit, credit, recon, itemId));
        }
        return out;
    }

    @Override
    public boolean hasDraftJournalEntriesThrough(CompanyId companyId, LocalDate toInclusive) {
        return journalEntryJpaRepository.existsByCompanyIdAndStatusAndEntryDateLessThan(
                companyId.getId(), JournalEntryStatus.DRAFT, toInclusive.plusDays(1).atStartOfDay());
    }
}
