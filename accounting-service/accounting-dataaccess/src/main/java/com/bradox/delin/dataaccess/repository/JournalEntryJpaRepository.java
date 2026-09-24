package com.bradox.delin.dataaccess.repository;

import com.bradox.delin.dataaccess.entity.JournalEntryEntity;
import com.bradox.delin.domain.core.ValueObject.JournalEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {

    List<JournalEntryEntity> findByCompanyId(UUID companyId);

    boolean existsByCompanyId(UUID companyId);

    List<JournalEntryEntity> findByCompanyIdAndJournalIdAndEntryDateBetween(
            UUID companyId, UUID journalId, LocalDateTime from, LocalDateTime to);

    boolean existsBySequenceNumberAndCompanyIdAndJournal_Id(
            String sequenceNumber, UUID companyId, UUID journalId);

    boolean existsByCompanyIdAndStatusAndEntryDateLessThan(
            UUID companyId, JournalEntryStatus status, LocalDateTime toExclusive);
}
