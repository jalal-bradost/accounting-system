package com.jalaldeveloper.accountingsystem.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntrySequenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JournalEntrySequenceJpaRepository extends JpaRepository<JournalEntrySequenceEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM JournalEntrySequenceEntity s WHERE s.companyId = :companyId AND s.journalId = :journalId AND s.periodKey = :periodKey")
    Optional<JournalEntrySequenceEntity> findByCompanyIdAndJournalIdAndPeriodKeyForUpdate(
            @Param("companyId") UUID companyId, @Param("journalId") UUID journalId, @Param("periodKey") String periodKey);
}
