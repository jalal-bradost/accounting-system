package com.bradox.erp.timesheet.dataaccess.repository;

import com.bradox.erp.timesheet.dataaccess.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {

    java.util.Optional<TaskEntity> findByCompanyIdAndRecordModelAndRecordId(UUID companyId, String recordModel, UUID recordId);

    List<TaskEntity> findByCompanyIdAndProjectId(UUID companyId, UUID projectId);

    List<TaskEntity> findByCompanyIdAndStatusInAndProjectIdIn(UUID companyId, Collection<String> statuses,
                                                              Collection<UUID> projectIds);
}
