package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.LaborCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LaborCategoryJpaRepository extends JpaRepository<LaborCategoryEntity, UUID> {

    List<LaborCategoryEntity> findByCompanyIdOrderByNameAsc(UUID companyId);
}
