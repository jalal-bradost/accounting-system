package com.jalaldeveloper.accountingsystem.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    @Modifying
    @Query("UPDATE JournalItemEntity i SET i.reconciliationId = :reconciliationId WHERE i.id IN :ids")
    void setReconciliationId(@Param("ids") List<UUID> ids, @Param("reconciliationId") UUID reconciliationId);

    @Modifying
    @Query("UPDATE JournalItemEntity i SET i.reconciliationId = NULL WHERE i.id IN :ids")
    void clearReconciliationId(@Param("ids") List<UUID> ids);
}
