package com.jalaldeveloper.accountingsystem.platform.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {

    Page<AuditLogEntity> findByCompanyIdAndModelNameAndRecordIdOrderByOccurredAtDesc(
            UUID companyId, String modelName, UUID recordId, Pageable pageable);

    Page<AuditLogEntity> findByCompanyIdOrderByOccurredAtDesc(UUID companyId, Pageable pageable);
}
