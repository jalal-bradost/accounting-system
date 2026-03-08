package com.jalaldeveloper.accountingsystem.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalJpaRepository extends JpaRepository<JournalEntity, UUID> {

    Optional<JournalEntity> findByCompanyIdAndCode(UUID companyId, String code);

    List<JournalEntity> findByCompanyId(UUID companyId);

    boolean existsByCompanyIdAndCode(UUID companyId, String code);
}
