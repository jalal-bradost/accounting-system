package com.jalaldeveloper.accountingsystem.platform.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RecordFollowerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecordFollowerJpaRepository extends JpaRepository<RecordFollowerEntity, UUID> {

    List<RecordFollowerEntity> findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtAsc(
            UUID companyId, String modelName, UUID recordId);

    long countByCompanyIdAndModelNameAndRecordId(UUID companyId, String modelName, UUID recordId);

    Optional<RecordFollowerEntity> findByCompanyIdAndModelNameAndRecordIdAndPartnerId(
            UUID companyId, String modelName, UUID recordId, UUID partnerId);
}
