package com.jalaldeveloper.accountingsystem.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalItemEntity;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Used for trial balance query and reconciliation updates. Individual item persistence
 * is otherwise handled via JournalEntryEntity cascade.
 */
public interface JournalItemJpaRepository extends JpaRepository<JournalItemEntity, UUID> {

    @Query("SELECT i.account.id, (SUM(i.debit) - SUM(i.credit)) FROM JournalItemEntity i "
            + "JOIN i.journalEntry e WHERE e.companyId = :companyId AND e.status = 'POSTED' "
            + "AND e.entryDate BETWEEN :fromDate AND :toDate GROUP BY i.account.id")
    List<Object[]> findTrialBalance(@Param("companyId") UUID companyId,
                                   @Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate);

    @Query("SELECT i.account.id, (SUM(i.debit) - SUM(i.credit)) FROM JournalItemEntity i "
            + "JOIN i.journalEntry e WHERE e.companyId = :companyId AND e.status = 'POSTED' "
            + "AND e.entryDate <= :asOf AND i.account.type IN :accountTypes GROUP BY i.account.id")
    List<Object[]> findBalancesUpTo(@Param("companyId") UUID companyId,
                                   @Param("asOf") LocalDate asOf,
                                   @Param("accountTypes") Collection<AccountType> accountTypes);

    @Query("SELECT i.account.id, e.id, e.entryDate, j.code, e.sequenceNumber, i.label, i.debit, i.credit "
            + "FROM JournalItemEntity i JOIN i.journalEntry e JOIN e.journal j "
            + "WHERE e.companyId = :companyId AND e.status = 'POSTED' "
            + "AND e.entryDate BETWEEN :fromDate AND :toDate "
            + "AND (:accountId IS NULL OR i.account.id = :accountId) "
            + "ORDER BY i.account.id, e.entryDate, e.id, i.id")
    List<Object[]> findGeneralLedgerLines(@Param("companyId") UUID companyId,
                                          @Param("fromDate") LocalDate fromDate,
                                          @Param("toDate") LocalDate toDate,
                                          @Param("accountId") UUID accountId);

    @Modifying
    @Query("UPDATE JournalItemEntity i SET i.reconciliationId = :reconciliationId WHERE i.id IN :ids")
    void setReconciliationId(@Param("ids") List<UUID> ids, @Param("reconciliationId") UUID reconciliationId);

    @Modifying
    @Query("UPDATE JournalItemEntity i SET i.reconciliationId = NULL WHERE i.id IN :ids")
    void clearReconciliationId(@Param("ids") List<UUID> ids);

    /**
     * Sum of (debit - credit) on posted journal items for the given partner, restricted to
     * a given account type (typically RECEIVABLE for AR or PAYABLE for AP balances).
     */
    @Query("SELECT COALESCE(SUM(i.debit) - SUM(i.credit), 0) FROM JournalItemEntity i "
            + "JOIN i.journalEntry e "
            + "WHERE e.companyId = :companyId AND e.status = 'POSTED' "
            + "AND COALESCE(i.partnerId, e.partnerId) = :partnerId "
            + "AND i.account.type = :accountType")
    java.math.BigDecimal sumPartnerBalanceByAccountType(
            @Param("companyId") UUID companyId,
            @Param("partnerId") UUID partnerId,
            @Param("accountType") AccountType accountType);
}
