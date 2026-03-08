package com.jalaldeveloper.accountingsystem.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {

    List<JournalEntryEntity> findByCompanyId(UUID companyId);

    List<JournalEntryEntity> findByCompanyIdAndJournalIdAndEntryDateBetween(
            UUID companyId, UUID journalId, LocalDate from, LocalDate to);

    boolean existsBySequenceNumberAndCompanyIdAndJournal_Id(
            String sequenceNumber, UUID companyId, UUID journalId);
}
