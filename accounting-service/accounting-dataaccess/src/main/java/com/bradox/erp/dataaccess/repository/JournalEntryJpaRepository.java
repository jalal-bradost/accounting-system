package com.bradox.erp.dataaccess.repository;

import com.bradox.erp.dataaccess.entity.JournalEntryEntity;
import com.bradox.erp.domain.core.ValueObject.JournalEntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {

    /**
     * Loads journal + items + accounts in one query. Required because
     * {@code spring.jpa.open-in-view=false} and the dataaccess mapper reads items
     * after the Spring Data repository method returns.
     */
    @Query("SELECT DISTINCT e FROM JournalEntryEntity e "
            + "LEFT JOIN FETCH e.journal "
            + "LEFT JOIN FETCH e.items i "
            + "LEFT JOIN FETCH i.account "
            + "WHERE e.companyId = :companyId")
    List<JournalEntryEntity> findByCompanyId(@Param("companyId") UUID companyId);

    @Query(
            value = "select e.id from JournalEntryEntity e where e.companyId = :companyId order by e.entryDate desc, e.sequenceNumber desc",
            countQuery = "select count(e) from JournalEntryEntity e where e.companyId = :companyId")
    Page<UUID> findIdsByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);

    @Query("SELECT DISTINCT e FROM JournalEntryEntity e "
            + "LEFT JOIN FETCH e.journal "
            + "LEFT JOIN FETCH e.items i "
            + "LEFT JOIN FETCH i.account "
            + "WHERE e.id in :ids")
    List<JournalEntryEntity> findByIdInWithDetails(@Param("ids") Collection<UUID> ids);

    @Query("SELECT DISTINCT e FROM JournalEntryEntity e "
            + "LEFT JOIN FETCH e.journal "
            + "LEFT JOIN FETCH e.items i "
            + "LEFT JOIN FETCH i.account "
            + "WHERE e.id = :id")
    Optional<JournalEntryEntity> findByIdWithDetails(@Param("id") UUID id);

    boolean existsByCompanyId(UUID companyId);

    @Query("SELECT DISTINCT e FROM JournalEntryEntity e "
            + "LEFT JOIN FETCH e.journal "
            + "LEFT JOIN FETCH e.items i "
            + "LEFT JOIN FETCH i.account "
            + "WHERE e.companyId = :companyId AND e.journal.id = :journalId "
            + "AND e.entryDate BETWEEN :from AND :to")
    List<JournalEntryEntity> findByCompanyIdAndJournalIdAndEntryDateBetween(
            @Param("companyId") UUID companyId,
            @Param("journalId") UUID journalId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    boolean existsBySequenceNumberAndCompanyIdAndJournal_Id(
            String sequenceNumber, UUID companyId, UUID journalId);

    boolean existsByCompanyIdAndStatusAndEntryDateLessThan(
            UUID companyId, JournalEntryStatus status, LocalDateTime toExclusive);
}
