package com.bradox.erp.project.dataaccess.repository;

import com.bradox.erp.project.dataaccess.entity.PrjProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrjProjectJpaRepository extends JpaRepository<PrjProjectEntity, UUID> {

    List<PrjProjectEntity> findByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<PrjProjectEntity> findByCompanyIdAndId(UUID companyId, UUID id);
}
