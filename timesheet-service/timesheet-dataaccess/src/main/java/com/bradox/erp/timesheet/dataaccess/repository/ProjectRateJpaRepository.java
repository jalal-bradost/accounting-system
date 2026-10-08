package com.bradox.erp.timesheet.dataaccess.repository;

import com.bradox.erp.timesheet.dataaccess.entity.ProjectRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProjectRateJpaRepository extends JpaRepository<ProjectRateEntity, UUID> {

    List<ProjectRateEntity> findByProjectId(UUID projectId);

    List<ProjectRateEntity> findByProjectIdIn(Collection<UUID> projectIds);

    void deleteByProjectId(UUID projectId);
}
