package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.PartnerLedgerRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalEntryJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalItemJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
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
                companyId.getId(), fromExclusive, tradeAccountTypes);
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
                companyId.getId(), fromInclusive, toInclusive, tradeAccountTypes);
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
                companyId.getId(), fromInclusive, toInclusive, partnerId, tradeAccountTypes);
        List<PartnerLedgerMovementRaw> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            UUID pid = (UUID) row[0];
            String nameHint = row[1] != null ? (String) row[1] : null;
            UUID entryId = (UUID) row[2];
            LocalDate entryDate = (LocalDate) row[3];
            String journalCode = (String) row[4];
            String seq = (String) row[5];
            String accountCode = (String) row[6];
            String accountName = (String) row[7];
            String label = row[8] != null ? (String) row[8] : "";
            BigDecimal debit = row[9] != null ? (BigDecimal) row[9] : BigDecimal.ZERO;
            BigDecimal credit = row[10] != null ? (BigDecimal) row[10] : BigDecimal.ZERO;
            UUID recon = (UUID) row[11];
            UUID itemId = (UUID) row[12];
            out.add(new PartnerLedgerMovementRaw(
                    pid, nameHint, entryId, entryDate, journalCode, seq, accountCode, accountName,
                    label, debit, credit, recon, itemId));
        }
        return out;
    }

    @Override
    public boolean hasDraftJournalEntriesThrough(CompanyId companyId, LocalDate toInclusive) {
        return journalEntryJpaRepository.existsByCompanyIdAndStatusAndEntryDateLessThanEqual(
                companyId.getId(), JournalEntryStatus.DRAFT, toInclusive);
    }
}
