package com.bradox.erp.accounting.service.domain.ports.output.repository;

import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Posted journal activity on trade (receivable / payable) accounts, resolved per journal line partner
 * or fallback to the entry header partner — the basis for an Odoo-style partner subsidiary ledger.
 */
public interface PartnerLedgerRepository {

    /**
     * Sum of (debit − credit) per partner for posted lines on {@code tradeAccountTypes} with entry date
     * strictly before {@code fromExclusive}.
     */
    Map<UUID, BigDecimal> sumOpeningBalanceByPartnerBefore(
            CompanyId companyId,
            LocalDate fromExclusive,
            List<AccountType> tradeAccountTypes);

    Map<UUID, PeriodDebitCredit> sumPeriodDebitCreditByPartner(
            CompanyId companyId,
            LocalDate fromInclusive,
            LocalDate toInclusive,
            List<AccountType> tradeAccountTypes);

    List<PartnerLedgerMovementRaw> listMovementsInPeriod(
            CompanyId companyId,
            LocalDate fromInclusive,
            LocalDate toInclusive,
            UUID partnerId,
            List<AccountType> tradeAccountTypes);

    boolean hasDraftJournalEntriesThrough(CompanyId companyId, LocalDate toInclusive);

    record PeriodDebitCredit(BigDecimal debit, BigDecimal credit) {}

    record PartnerLedgerMovementRaw(
            UUID partnerId,
            String partnerNameHint,
            UUID journalEntryId,
            LocalDateTime entryDate,
            String journalCode,
            String journalName,
            String sequenceNumber,
            String accountCode,
            String accountName,
            String label,
            BigDecimal debit,
            BigDecimal credit,
            UUID reconciliationId,
            UUID journalItemId) {}
}
