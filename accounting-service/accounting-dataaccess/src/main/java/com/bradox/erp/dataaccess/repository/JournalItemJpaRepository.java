package com.bradox.erp.dataaccess.repository;

import com.bradox.erp.dataaccess.entity.JournalItemEntity;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
            + "AND e.entryDate >= :fromInclusive AND e.entryDate < :toExclusive GROUP BY i.account.id")
    List<Object[]> findTrialBalance(@Param("companyId") UUID companyId,
                                   @Param("fromInclusive") LocalDateTime fromInclusive,
                                   @Param("toExclusive") LocalDateTime toExclusive);

    @Query("SELECT i.account.id, (SUM(i.debit) - SUM(i.credit)) FROM JournalItemEntity i "
            + "JOIN i.journalEntry e WHERE e.companyId = :companyId AND e.status = 'POSTED' "
            + "AND e.entryDate < :asOfExclusive AND i.account.type IN :accountTypes GROUP BY i.account.id")
    List<Object[]> findBalancesUpTo(@Param("companyId") UUID companyId,
                                   @Param("asOfExclusive") LocalDateTime asOfExclusive,
                                   @Param("accountTypes") Collection<AccountType> accountTypes);

    @Query("SELECT i.account.id, e.id, e.entryDate, j.code, e.sequenceNumber, i.label, i.debit, i.credit "
            + "FROM JournalItemEntity i JOIN i.journalEntry e JOIN e.journal j "
            + "WHERE e.companyId = :companyId AND e.status = 'POSTED' "
            + "AND e.entryDate >= :fromInclusive AND e.entryDate < :toExclusive "
            + "AND (:accountId IS NULL OR i.account.id = :accountId) "
            + "ORDER BY i.account.id, e.entryDate, e.id, i.id")
    List<Object[]> findGeneralLedgerLines(@Param("companyId") UUID companyId,
                                          @Param("fromInclusive") LocalDateTime fromInclusive,
                                          @Param("toExclusive") LocalDateTime toExclusive,
                                          @Param("accountId") UUID accountId);

    @Modifying
    @Query("UPDATE JournalItemEntity i SET i.reconciliationId = :reconciliationId WHERE i.id IN :ids")
    void setReconciliationId(@Param("ids") List<UUID> ids, @Param("reconciliationId") UUID reconciliationId);

    @Modifying
    @Query("UPDATE JournalItemEntity i SET i.reconciliationId = NULL WHERE i.id IN :ids")
    void clearReconciliationId(@Param("ids") List<UUID> ids);

    @Query("SELECT i.id FROM JournalItemEntity i WHERE i.reconciliationId IN :reconciliationIds")
    List<UUID> findIdsByReconciliationIdIn(@Param("reconciliationIds") Collection<UUID> reconciliationIds);

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

    @Query("SELECT COALESCE(i.partnerId, e.partnerId), COALESCE(SUM(i.debit - i.credit), 0) "
            + "FROM JournalItemEntity i JOIN i.journalEntry e JOIN i.account a "
            + "WHERE e.companyId = :companyId AND e.status = 'POSTED' "
            + "AND a.type IN :tradeTypes "
            + "AND COALESCE(i.partnerId, e.partnerId) IS NOT NULL "
            + "AND e.entryDate < :fromExclusive "
            + "GROUP BY COALESCE(i.partnerId, e.partnerId)")
    List<Object[]> sumOpeningBalanceByPartnerBefore(
            @Param("companyId") UUID companyId,
            @Param("fromExclusive") LocalDateTime fromExclusive,
            @Param("tradeTypes") Collection<AccountType> tradeTypes);

    @Query("SELECT COALESCE(i.partnerId, e.partnerId), COALESCE(SUM(i.debit), 0), COALESCE(SUM(i.credit), 0) "
            + "FROM JournalItemEntity i JOIN i.journalEntry e JOIN i.account a "
            + "WHERE e.companyId = :companyId AND e.status = 'POSTED' "
            + "AND a.type IN :tradeTypes "
            + "AND COALESCE(i.partnerId, e.partnerId) IS NOT NULL "
            + "AND e.entryDate >= :fromInclusive AND e.entryDate < :toExclusive "
            + "GROUP BY COALESCE(i.partnerId, e.partnerId)")
    List<Object[]> sumPeriodDebitCreditByPartner(
            @Param("companyId") UUID companyId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("tradeTypes") Collection<AccountType> tradeTypes);

    @Query("SELECT COALESCE(i.partnerId, e.partnerId), "
            + "COALESCE(NULLIF(TRIM(i.partnerName), ''), NULLIF(TRIM(e.partnerName), '')), "
            + "e.id, e.entryDate, j.code, j.name, e.sequenceNumber, a.code, a.name, i.label, i.debit, i.credit, "
            + "i.reconciliationId, i.id "
            + "FROM JournalItemEntity i JOIN i.journalEntry e JOIN e.journal j JOIN i.account a "
            + "WHERE e.companyId = :companyId AND e.status = 'POSTED' "
            + "AND a.type IN :tradeTypes "
            + "AND COALESCE(i.partnerId, e.partnerId) IS NOT NULL "
            + "AND e.entryDate >= :fromInclusive AND e.entryDate < :toExclusive "
            + "AND COALESCE(i.partnerId, e.partnerId) = :partnerId "
            + "ORDER BY e.entryDate, e.id, i.id")
    List<Object[]> listPartnerTradeMovementsInPeriod(
            @Param("companyId") UUID companyId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("partnerId") UUID partnerId,
            @Param("tradeTypes") Collection<AccountType> tradeTypes);
}
