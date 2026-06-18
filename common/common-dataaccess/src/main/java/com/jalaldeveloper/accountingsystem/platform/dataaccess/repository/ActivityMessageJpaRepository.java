package com.jalaldeveloper.accountingsystem.platform.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ActivityMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivityMessageJpaRepository extends JpaRepository<ActivityMessageEntity, UUID> {

    Page<ActivityMessageEntity> findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(
            UUID companyId, String modelName, UUID recordId, Pageable pageable);

    Page<ActivityMessageEntity> findByCompanyIdAndAssigneeIdAndCompletedAtIsNullOrderByDueDateAsc(
            UUID companyId, String assigneeId, Pageable pageable);
}
