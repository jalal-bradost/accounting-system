package com.jalaldeveloper.accountingsystem.platform.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RecordAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecordAttachmentJpaRepository extends JpaRepository<RecordAttachmentEntity, UUID> {

    List<RecordAttachmentEntity> findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(
            UUID companyId, String modelName, UUID recordId);

    long countByCompanyIdAndModelNameAndRecordId(UUID companyId, String modelName, UUID recordId);
}
