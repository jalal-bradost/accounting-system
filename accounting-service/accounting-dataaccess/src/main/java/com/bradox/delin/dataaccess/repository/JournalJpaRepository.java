package com.bradox.delin.dataaccess.repository;

import com.bradox.delin.dataaccess.entity.JournalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalJpaRepository extends JpaRepository<JournalEntity, UUID> {

    Optional<JournalEntity> findByCompanyIdAndCode(UUID companyId, String code);

    List<JournalEntity> findByCompanyId(UUID companyId);

    boolean existsByCompanyIdAndCode(UUID companyId, String code);
}
