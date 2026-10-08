package com.bradox.erp.timesheet.dataaccess.repository;

import com.bradox.erp.timesheet.dataaccess.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, UUID> {

    List<ProjectEntity> findByCompanyId(UUID companyId);

    List<ProjectEntity> findByCompanyIdAndStatus(UUID companyId, String status);

    Optional<ProjectEntity> findByCompanyIdAndSystemKey(UUID companyId, String systemKey);

    boolean existsByCompanyIdAndCodeNormalized(UUID companyId, String codeNormalized);

    boolean existsByCompanyIdAndCodeNormalizedAndIdNot(UUID companyId, String codeNormalized, UUID id);
}
